local M = {}

function M.show_menu()
  local options = {
    "Generate Getters and Setters",
  }

  vim.ui.select(options, { prompt = "Choose a refactor action:" }, function(choice)
    if not choice then
      vim.schedule(function()
        vim.notify("Cancelled", vim.log.levels.INFO)
      end)
      return
    end

    -- use dummy backend instead of real backend
    local backend = require("refactor.dummy_backend")

    -- simulate getting buffer text
    local lines = vim.api.nvim_buf_get_lines(0, 0, -1, false)
    local source = table.concat(lines, "\n")

    backend.send_request({
      command = choice,
      source = source,
    })

    vim.schedule(function()
      vim.notify("You chose: " .. choice)
    end)
  end)
end

return M

