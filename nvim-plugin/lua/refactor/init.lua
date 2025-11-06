local backend = require("refactor.backend")
local ui = require("refactor.ui")

local M = {}

M.start_backend = backend.start_backend()
M.send_request = backend.send_request()
M.menu = ui.show_menu()

backend.start_backend()

vim.api.nvim_create_user_command("RefactorMenu", function()
  ui.show_menu()
end, {})

vim.notify("java-refactor plugin loaded!", vim.log.levels.INFO)

