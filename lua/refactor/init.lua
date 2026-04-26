local backend = require("refactor.backend")
local ui = require("refactor.ui")
local config = require("refactor.config")
local settings = require("refactor.settings")

local M = {}

local function clear_keymaps()
	local keymap_opts = { buffer = 0 }
	for _, action in pairs(config.defaults.keybindings) do
		pcall(vim.keymap.del, "n", action)
		pcall(vim.keymap.del, "v", action)
	end
end

function M.setup(user_config)
	config.setup(user_config)
	M.setup_keymaps()
end

function M.setup_keymaps()
	clear_keymaps()

	vim.keymap.set("n", config.get_keybinding("menu"), function()
		ui.show_menu()
	end, { desc = "Java Refactor Menu" })

	vim.keymap.set("n", config.get_keybinding("generate_getters_setters"), function()
		local actions = require("refactor.actions")
		actions.generate_getters_setters()
	end, { desc = "Generate Getters/Setters" })

	vim.keymap.set("n", config.get_keybinding("generate_to_string"), function()
		local actions = require("refactor.actions")
		actions.generate_to_string()
	end, { desc = "Generate toString" })

	vim.keymap.set("n", config.get_keybinding("inline_method"), function()
		local actions = require("refactor.actions")
		actions.inline_method()
	end, { desc = "Inline Method" })

	vim.keymap.set("n", config.get_keybinding("encapsulate_field"), function()
		local actions = require("refactor.actions")
		actions.encapsulate_field()
	end, { desc = "Encapsulate Field" })

	vim.keymap.set("v", config.get_keybinding("extract_variable"), function()
		local actions = require("refactor.actions")
		actions.extract_variable()
	end, { desc = "Extract Variable" })

	vim.keymap.set("v", config.get_keybinding("extract_method"), function()
		local actions = require("refactor.actions")
		actions.extract_method()
	end, { desc = "Extract Method" })

	vim.keymap.set("n", config.get_keybinding("extract_interface"), function()
		local actions = require("refactor.actions")
		actions.extract_interface()
	end, { desc = "Extract Interface" })

	vim.keymap.set("n", config.get_keybinding("ai_find_usages"), function()
		local ai_find_usages = require("refactor.ai_find_usages")
		ai_find_usages.ai_find_usages()
	end, { desc = "AI Find Usages" })

	vim.keymap.set("n", config.get_keybinding("rename"), function()
		local actions = require("refactor.actions")
		actions.rename()
	end, { desc = "Rename" })

	vim.keymap.set("n", config.get_keybinding("pull_up"), function()
		local actions = require("refactor.actions")
		actions.pull_up()
	end, { desc = "Pull Up" })

	vim.keymap.set("n", config.get_keybinding("push_down"), function()
		local actions = require("refactor.actions")
		actions.push_down()
	end, { desc = "Push Down" })

	vim.keymap.set("n", config.get_keybinding("settings"), function()
		settings.show_settings()
	end, { desc = "Refactor Settings" })
end

function M.start_backend()
	backend.start_backend()
end

function M.menu()
	ui.show_menu()
end

function M.help()
	ui.show_help()
end

function M.settings()
	settings.show_settings()
end

vim.api.nvim_create_user_command("RefactorMenu", M.menu, {})
vim.api.nvim_create_user_command("RefactorHelp", M.help, {})
vim.api.nvim_create_user_command("RefactorSettings", M.settings, {})

M.setup()

return M
