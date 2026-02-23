local backend = require("refactor.backend")
local ui = require("refactor.ui")

local M = {}

-- ui funcs

function M.start_backend()
	backend.start_backend()
end

function M.menu()
	ui.show_menu()
end

function M.help()
	ui.show_help()
end

-- Refactor Commands

vim.keymap.set('n', '<leader>jf', function()
	ui.show_menu()
end, { desc = 'Java Refactor Menu' })

vim.keymap.set('v', '<leader>J', function()
	ui.show_menu()
end, { desc = 'Java Refactor Menu' })

-- Quick motions for common refactoring

--Make this customisable
--Move to seperate file

vim.keymap.set('n', '<leader>gg', function()
	local actions = require("refactor.actions")
	actions.generate_getters_setters()
end, { desc = 'Generate Getters/Setters' })

vim.keymap.set('n', '<leader>gt', function()
	local actions = require("refactor.actions")
	actions.generate_to_string()
end, { desc = 'Generate toString' })

vim.keymap.set('n', '<leader>im', function()
	local actions = require("refactor.actions")
	actions.inline_method()
end, { desc = 'Inline Method' })

vim.keymap.set('v', '<leader>ev', function()
	local actions = require("refactor.actions")
	actions.extract_variable()
end, { desc = 'Extract Variable' })

vim.keymap.set('v', '<leader>em', function()
	local actions = require("refactor.actions")
	actions.extract_method()
end, { desc = 'Extract Method' })

--vim.notify("java-refactor plugin loaded!", vim.log.levels.INFO)

--not needed 
--vim.api.nvim_create_user_command("RefactorStart", M.start_backend, {})

--user commands
vim.api.nvim_create_user_command("RefactorMenu", M.menu, {})
vim.api.nvim_create_user_command("RefactorHelp", M.help, {})

return M
