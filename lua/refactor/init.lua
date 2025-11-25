local backend = require("refactor.backend")
local ui = require("refactor.ui")

local M = {}

function M.start_backend()
  backend.start_backend()
end

function M.menu()
  ui.show_menu()
end

--vim.notify("java-refactor plugin loaded!", vim.log.levels.INFO)
vim.api.nvim_create_user_command("RefactorStart", M.start_backend, {})
vim.api.nvim_create_user_command("RefactorMenu", M.menu, {})

return M
