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

-- Quick motions for common refactoring

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

vim.api.nvim_set_keymap('v', '<leader>ev', "<cmd>lua require('refactor.actions').extract_variable()<cr>", { noremap = true, silent = true })
vim.api.nvim_set_keymap('v', '<leader>er', "<cmd>lua require('refactor.actions').extract_method()<cr>", { noremap = true, silent = true })

--vim.notify("java-refactor plugin loaded!", vim.log.levels.INFO)

--not needed 
--vim.api.nvim_create_user_command("RefactorStart", M.start_backend, {})

--user commands
vim.api.nvim_create_user_command("RefactorMenu", M.menu, {})
vim.api.nvim_create_user_command("RefactorHelp", M.help, {})

return M
