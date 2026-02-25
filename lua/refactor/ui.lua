local Menu = require("nui.menu")
local Popup = require("nui.popup")
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
	local popup = Popup({
		position = "50%",
		size = {
			width = 50,
			height = 10,
		},
		border = {
			style = "rounded",
			text = {
				top = "[ Commands ]",
				top_align = "center",
			},
		},
		win_options = {
			winhighlight = "Normal:Normal,FloatBorder:Normal",
		},
	})

	popup:mount()

	-- Focus the popup window
	vim.api.nvim_set_current_win(popup.winid)

	-- Set buffer lines after mounting
	local buf = popup.bufnr
	vim.api.nvim_buf_set_lines(buf, 0, -1, false, {
		"  Generate Getters/Setters          <leader>gg",
		"  Generate toString                 <leader>gt",
		"  Extract Method                    <leader>er",
		"  Extract Variable                  <leader>ev",
		"  Inline Method                     <leader>im",
		"  Menu                              <leader>jf",
		"",
		"  :RefactorHelp  :RefactorMenu",
		"",
		"  q or esc to exit",
	})

	popup:map("n", "q", function()
		popup:unmount()
	end)

	popup:map("n", "<Esc>", function()
		popup:unmount()
	end)
end

return M

