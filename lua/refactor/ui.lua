local Menu = require("nui.menu")
local Popup = require("nui.popup")
local Split = require("nui.split")
local actions = require("refactor.actions")
local backend = require("refactor.backend")

local M = {}

local function is_java_file()
	local buf = vim.api.nvim_get_current_buf()
	local filetype = vim.bo[buf].filetype
	if filetype ~= "java" then
		vim.notify("Not a Java file", vim.log.levels.WARN)
		return false
	end
	return true
end

function M.show_menu()
	if not is_java_file() then return end
	
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
			Menu.item("Flame Graph"),
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
			elseif item.text == "Flame Graph" then
				actions.flame_graph()
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
		"  Flame Graph                       <leader>pf",
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

local flame_split = nil

function M.show_flame_graph(flame_data)
	if flame_split and flame_split:is_mounted() then
		flame_split:unmount()
	end
	
	flame_split = Split({
		orient = "left",
		size = 45,
		border = {
			style = "rounded",
			text = {
				top = " 🔥 Flame Graph ",
				top_align = "center",
			},
		},
		win_options = {
			winhighlight = "Normal:Normal,FloatBorder:Normal",
			wrap = false,
		},
	})
	
	flame_split:mount()
	
	vim.api.nvim_set_current_win(flame_split.winid)
	
	local lines = vim.split(flame_data, "\n")
	vim.api.nvim_buf_set_lines(flame_split.bufnr, 0, -1, false, lines)
	
	vim.bo[flame_split.bufnr].modifiable = false
	vim.bo[flame_split.bufnr].readonly = true
	vim.bo[flame_split.bufnr].filetype = "diff"
	
	flame_split:map("n", "q", function()
		flame_split:unmount()
		flame_split = nil
	end, { nowait = true })
	
	flame_split:map("n", "<Esc>", function()
		flame_split:unmount()
		flame_split = nil
	end, { nowait = true })
end

return M

