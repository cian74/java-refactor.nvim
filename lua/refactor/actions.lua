local backend = require("refactor.backend")
local Menu = require("nui.menu")
local M = {}

local field_selection_state = {
	fields = {},
	selected = {},
	source = "",
	menu = nil
}

function M.generate_getters_setters()
	-- Get current buffer content
	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines, "\n")
	
	field_selection_state.source = source
	field_selection_state.selected = {}

	backend.send_request({
		command = "list_fields",
		source = source
	})
end

function M.show_field_selection_menu(fields)
    if not fields or #fields == 0 then
        vim.notify("No private fields found", vim.log.levels.WARN)
        return
    end
    
    -- Only reset fields if this is a new menu, not a refresh
    if not field_selection_state.fields or #field_selection_state.fields == 0 then
        field_selection_state.fields = fields
        field_selection_state.selected = {}
    end
    
    local menu_items = {}
    for i, field in ipairs(field_selection_state.fields) do
        local checkbox = field_selection_state.selected[i] and "[x]" or "[ ]"
        table.insert(menu_items, Menu.item(checkbox .. " " .. field, { field_index = i }))
    end
    
    local menu = Menu({
        position = "50%",
        size = {
            width = 50,
            height = math.min(#field_selection_state.fields + 4, 20),
        },
        border = {
            style = "rounded",
            text = {
                top = "[ Select Fields - Space to toggle, Enter to confirm ]",
                top_align = "center",
            },
        },
        win_options = {
            winhighlight = "Normal:Normal,FloatBorder:Normal",
        },
    }, {
        lines = menu_items,
        max_width = 50,
        keymap = {
            focus_next = { "j", "<Down>", "<Tab>" },
            focus_prev = { "k", "<Up>", "<S-Tab>" },
            close = { "<Esc>", "<C-c>" },
            submit = { "<CR>" },
        },
        on_close = function()
            field_selection_state.menu = nil
            field_selection_state.fields = {}
            field_selection_state.selected = {}
        end,
        on_submit = function(item)
            -- Get selected fields
            local selected_fields = {}
            for i, _ in pairs(field_selection_state.selected) do
                table.insert(selected_fields, field_selection_state.fields[i])
            end
            
            if #selected_fields == 0 then
                vim.notify("No fields selected", vim.log.levels.WARN)
                return
            end
            
			if field_selection_state.menu then
				
				field_selection_state.menu:unmount()
			end
            
            -- Generate getters/setters for selected fields
            backend.send_request({
                command = "generate_field_getters_setters",
                source = field_selection_state.source,
                selected_fields = selected_fields
            })
            
            -- Reset state
            field_selection_state.fields = {}
            field_selection_state.selected = {}
        end,
    })
    
    field_selection_state.menu = menu
    menu:mount()
    
    -- Add space key mapping AFTER mounting
    vim.api.nvim_buf_set_keymap(
        menu.bufnr,
        'n',
        '<Space>',
        '',
        {
            noremap = true,
            silent = true,
            callback = function()
                local current_line = vim.api.nvim_win_get_cursor(menu.winid)[1]
                local field_index = current_line
                
                -- Toggle selection
                if field_selection_state.selected[field_index] then
                    field_selection_state.selected[field_index] = nil
                else
                    field_selection_state.selected[field_index] = true
                end
                
                -- Store cursor position
                local cursor_pos = vim.api.nvim_win_get_cursor(menu.winid)
                
                -- Refresh menu
                menu:unmount()
                M.show_field_selection_menu(field_selection_state.fields)
                
                -- Restore cursor position
                vim.schedule(function()
                    if field_selection_state.menu and field_selection_state.menu.winid then
                        vim.api.nvim_win_set_cursor(field_selection_state.menu.winid, cursor_pos)
                    end
                end)
            end
        }
    )
end

function M.extract_method()
	local start_line = vim.fn.line("'<") - 1
	local end_line = vim.fn.line("'>") - 1
	local start_col = vim.fn.col("'<") - 1
	local end_col = vim.fn.col("'>")

	local highlighted

	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, start_line, end_line + 1, false)

	--handling single line selection
	if start_line == end_line then
		highlighted = string.sub(lines[1], start_col + 1, end_col)
	else
		lines[1] = string.sub(lines[1],start_col + 1)
		lines[#lines] = string.sub(lines[#lines],1,end_col)
		highlighted = table.concat(lines, "\n")
	end

	--print("Highlighted text: '" .. highlighted .. "'")
	--print("==================")

	local current_lines = vim.api.nvim_buf_get_lines(buf,0,-1,false)
	local current_source = table.concat(current_lines, "\n")

	vim.ui.input({
		prompt = 'Enter Method name: ',
		default = 'extractedMethod'
	}, function(method_name)
		if not method_name or method_name == "" then
			vim.notify("Cancelled",vim.log.levels.INFO)
			return
		end
		backend.send_request({
			command = "extract_method",
			source = current_source,
			start_line = start_line + 1,
			end_line = end_line + 1,
			method_name = method_name,
			highlighted = highlighted
		})
	end)
end



return M
