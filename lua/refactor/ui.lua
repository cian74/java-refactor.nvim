local Menu = require("nui.menu")
local actions = require("refactor.actions")
local backend = require("refactor.backend")

local M = {}

function M.show_menu()
	-- Ensure backend is running before showing menu
	backend.start_backend()

	local menu = Menu({
		position = "50%",
		size = {
			width = 40,
			height = 8,
		},
		border = {
			style = "rounded",
			text = {
				top = "[ Refactor Options ]",
				top_align = "center",
			},
		},
		win_options = {
			winhighlight = "Normal:Normal,FloatBorder:Normal",
		},
	}, {
		lines = {
			Menu.item("Generate Getters and Setters"),
			Menu.item("Generate toString"),
			Menu.item("Extract Method"),
			Menu.item("Extract Variable"),
			Menu.item("Inline Method"),
			-- Menu.item("Generate Constructor"),
		},
		max_width = 30,
		keymap = {
			focus_next = { "j", "<Down>", "<Tab>" },
			focus_prev = { "k", "<Up>", "<S-Tab>" },
			close = { "<Esc>", "<C-c>" },
			submit = { "<CR>", "<Space>" },
		},
		on_close = function()
			vim.notify("Menu closed", vim.log.levels.INFO)
		end,
		on_submit = function(item)
			vim.notify("Selected: " .. item.text, vim.log.levels.INFO)
			if item.text == "Generate Getters and Setters" then
				actions.generate_getters_setters()
			elseif item.text == "Generate toString" then
				actions.generate_to_string()
			elseif item.text == "Extract Method" then
				actions.extract_method()
			elseif item.text == "Inline Method" then
				actions.inline_method()
			elseif item.text == "Extract Variable" then
				actions.extract_variable()
			end
		end,
	})

	menu:mount()
end

function M.show_help()
	local help_text = {
		"",
		"═══════════════════════════════════════════════════════════════════════",
		"                      JAVA REFACTOR COMMANDS",
		"═══════════════════════════════════════════════════════════════════════",
		"",
		"  Commands:",
		"──────────────────────────────────────────────────────────────────────",
		"  :RefactorStart    - Start the Java backend server",
		"  :RefactorMenu    - Open the refactoring menu",
		"  :RefactorHelp    - Show this help menu",
		"",
		"  Quick Keybindings:",
		"──────────────────────────────────────────────────────────────────────",
		"  <leader>gg    - Generate Getters/Setters (normal)",
		"  <leader>gt    - Generate toString (normal)",
		"  <leader>im    - Inline Method (normal, cursor on method)",
		"  <leader>ev    - Extract Variable (visual, select expression)",
		"  <leader>em    - Extract Method (visual, select code)",
		"  <leader>jf    - Open menu (normal/visual)",
		"",
		"  Features:",
		"──────────────────────────────────────────────────────────────────────",
		"  Generate Getters and Setters",
		"    → Generates getter and setter methods for private fields",
		"    → Select which fields to include via menu",
		"",
		"  Generate toString",
		"    → Generates a toString() method with all private fields",
		"    → Format: ClassName{field1=value1, field2=value2}",
		"",
		"  Extract Method",
		"    → Extracts selected code into a new method",
		"    → Enter method name when prompted",
		"",
		"  Extract Variable",
		"    → Extracts selected expression to a local variable",
		"    → Enter variable name when prompted",
		"",
		"  Inline Method",
		"    → Inlines a method at its call sites and removes the method",
		"    → Works best with simple methods (single return statement)",
		"",
		"  Press q or <Esc> to close | j/k to scroll",
		"",
	}

	-- Create a floating window for the help
	local buf = vim.api.nvim_create_buf(false, true)
	vim.api.nvim_buf_set_lines(buf, 0, -1, false, help_text)

	local width = 70
	local height = 20

	local opts = {
		relative = "editor",
		width = width,
		height = height,
		row = math.floor((vim.o.lines - height) / 2),
		col = math.floor((vim.o.columns - width) / 2),
		style = "minimal",
		border = "rounded",
	}

	local win = vim.api.nvim_open_win(buf, true, opts)

	-- Enable scrolling
	vim.wo[win].scrolloff = 999
	vim.wo[win].wrap = false

	-- Set buffer options
	vim.bo[buf].modifiable = false
	vim.bo[buf].readonly = true
	vim.bo[buf].filetype = "markdown"

	-- Close on escape or q
	vim.keymap.set('n', 'q', function()
		vim.api.nvim_win_close(win, true)
	end, { buffer = buf, nowait = true })

	vim.keymap.set('n', '<Esc>', function()
		vim.api.nvim_win_close(win, true)
	end, { buffer = buf, nowait = true })

	-- Scroll up/down
	local total_lines = #help_text
	vim.keymap.set('n', 'j', function()
		local cursor = vim.api.nvim_win_get_cursor(win)
		if cursor[1] < total_lines then
			vim.api.nvim_win_set_cursor(win, { cursor[1] + 1, cursor[2] })
		end
	end, { buffer = buf, nowait = true })

	vim.keymap.set('n', 'k', function()
		local cursor = vim.api.nvim_win_get_cursor(win)
		if cursor[1] > 1 then
			vim.api.nvim_win_set_cursor(win, { cursor[1] - 1, cursor[2] })
		end
	end, { buffer = buf, nowait = true })
end

return M

