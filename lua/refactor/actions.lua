local backend = require("refactor.backend")
local Menu = require("nui.menu")
local M = {}

local state = {
	fields = {},
	selected = {},
	source = "",
	menu = nil,
	current_index = 1,
	cursor_autocmd = nil,
}

function M.generate_getters_setters()
	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)

	state.source = table.concat(lines, "\n")
	state.selected = {}

	backend.send_request({
		command = "list_fields",
		source = state.source,
	})
end

function M.show_field_selection_menu(fields)
	if not fields or #fields == 0 then
		vim.notify("No private fields found", vim.log.levels.WARN)
		return
	end

	state.fields = fields
	state.selected = {}
	state.current_index = 1

	local function make_items()
		local items = {}
		for i, field in ipairs(state.fields) do
			local checkbox = state.selected[i] and "[x]" or "[ ]"
			items[i] = Menu.item(checkbox .. " " .. field)
		end
		return items
	end

	local menu = Menu({
		position = "50%",
		size = {
			width = 50,
			height = math.min(#fields + 4, 20),
		},
		border = {
			style = "rounded",
			text = {
				top = "[ Select Fields – Space to toggle, Enter to confirm ]",
				top_align = "center",
			},
		},
	}, {
		lines = make_items(),
		keymap = {
			focus_next = { "j", "<Down>", "<Tab>" },
			focus_prev = { "k", "<Up>", "<S-Tab>" },
			close = { "<Esc>", "<C-c>" },
			submit = { "<CR>" },
		},
		on_close = function()
			if state.cursor_autocmd then
				pcall(vim.api.nvim_del_autocmd, state.cursor_autocmd)
				state.cursor_autocmd = nil
			end

			state.menu = nil
			state.fields = {}
			state.selected = {}
		end,
		on_submit = function()
			local selected_fields = {}

			for i, v in pairs(state.selected) do
				if v then
					table.insert(selected_fields, state.fields[i])
				end
			end

			if #selected_fields == 0 then
				vim.notify("No fields selected", vim.log.levels.WARN)
				return
			end

			if state.menu then
				state.menu:unmount()
				state.menu = nil
			end

			backend.send_request({
				command = "generate_field_getters_setters",
				source = state.source,
				selected_fields = selected_fields,
			})

			state.fields = {}
			state.selected = {}
		end,
	})

	state.menu = menu
	menu:mount()

	-- Set proper buffer options to avoid readonly warnings
	vim.bo[menu.bufnr].modifiable = true
	vim.bo[menu.bufnr].readonly = false

	state.cursor_autocmd = vim.api.nvim_create_autocmd("CursorMoved", {
		buffer = menu.bufnr,
		callback = function()
			state.current_index =
				vim.api.nvim_win_get_cursor(menu.winid)[1]
		end,
	})

	vim.keymap.set("n", "<Space>", function()
		local i = state.current_index
		if not state.fields[i] then return end

		state.selected[i] = not state.selected[i]

		local checkbox = state.selected[i] and "[x]" or "[ ]"
		local line = checkbox .. " " .. state.fields[i]

		-- ensure buffer is modifiable
		vim.bo[menu.bufnr].modifiable = true
		vim.bo[menu.bufnr].readonly = false
		
		--updates lines
		vim.api.nvim_buf_set_lines(menu.bufnr, i - 1, i, false, { line })
	end, { buffer = menu.bufnr, nowait = true })
end

function M.extract_method()
	-- Check if there's a visual selection
	local mode = vim.fn.mode()
	if not mode:match('v') then
		vim.notify("Select code in visual mode first", vim.log.levels.WARN)
		return
	end
	
	local start_line = vim.fn.line("'<") - 1
	local end_line = vim.fn.line("'>") - 1
	local start_col = vim.fn.col("'<") - 1
	local end_col = vim.fn.col("'>")
	
	if start_line < 0 or end_line < start_line then
		vim.notify("Invalid selection", vim.log.levels.WARN)
		return
	end

	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, start_line, end_line + 1, false)
	
	if not lines or #lines == 0 then
		vim.notify("No lines selected", vim.log.levels.WARN)
		return
	end

	local highlighted
	if start_line == end_line then
		highlighted = string.sub(lines[1], start_col + 1, end_col)
	else
		lines[1] = string.sub(lines[1], start_col + 1)
		lines[#lines] = string.sub(lines[#lines], 1, end_col)
		highlighted = table.concat(lines, "\n")
	end

	local source = table.concat(
		vim.api.nvim_buf_get_lines(buf, 0, -1, false),
		"\n"
	)

	-- Use vim.cmd for input which is more synchronous
	vim.cmd('call inputsave()')
	vim.cmd('let g:method_name = input("Enter Method name: ", "extractedMethod")')
	vim.cmd('call inputrestore()')
	
	local method_name = vim.g.method_name
	vim.g.method_name = nil
	
	if not method_name or method_name == "" then return end

	backend.send_request({
		command = "extract_method",
		source = source,
		start_line = start_line + 1,
		end_line = end_line + 1,
		method_name = method_name,
		highlighted = highlighted,
	})
end

function M.inline_method()
	local buf = vim.api.nvim_get_current_buf()
	local cursor_line = vim.api.nvim_win_get_cursor(0)[1]
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines, "\n")

	backend.send_request({
		command = "inline_method",
		source = source,
		start_line = cursor_line,
	})
end

function M.generate_to_string()
	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines, "\n")

	backend.send_request({
		command = "generate_toString",
		source = source,
	})
end

function M.extract_variable()
	-- Check if there's a visual selection
	local mode = vim.fn.mode()
	if not mode:match('v') then
		vim.notify("Select code in visual mode first", vim.log.levels.WARN)
		return
	end
	
	local buf = vim.api.nvim_get_current_buf()
	local start_pos = vim.fn.getpos("'<")
	local end_pos = vim.fn.getpos("'>")
	local highlighted = vim.api.nvim_buf_get_text(buf, start_pos[2] - 1, start_pos[3] - 1, end_pos[2] - 1, end_pos[3], {})[1]
	
	if not highlighted then
		vim.notify("No text selected", vim.log.levels.WARN)
		return
	end
	
	local start_line = start_pos[2]
	
	-- Use vim.cmd for input which is more synchronous
	vim.cmd('call inputsave()')
	vim.cmd('let g:var_name = input("Enter variable name: ")')
	vim.cmd('call inputrestore()')
	
	local var_name = vim.g.var_name
	vim.g.var_name = nil
	
	if not var_name or var_name == "" then
		vim.notify("No variable name provided", vim.log.levels.WARN)
		return
	end
	
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines, "\n")
	
	backend.send_request({
		command = "extract_variable",
		source = source,
		highlighted = highlighted,
		var_name = var_name,
		start_line = start_line,
	})
end

return M

