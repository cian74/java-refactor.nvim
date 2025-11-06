local backend = require("refactor.backend")
local ui = require("refactor.ui")

backend.start_backend()

vim.api.nvim_create_user_command("RefactorMenu", function()
  ui.show_menu()
end, {})

vim.notify("Java Refactor plugin loaded!", vim.log.levels.INFO)

