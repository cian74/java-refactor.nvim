local Menu = require("nui.menu")
local event = require("nui.utils.autocmd").event

local M = {}

function M.show_menu()
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
      Menu.item("Rename Variable"),
      Menu.item("Extract Method"),
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
      vim.notify("Selected: " .. item.text)

      -- You can connect to your backend here
      local backend = require("refactor.backend")
      backend.send_request({
        command = item.text,
        source = table.concat(vim.api.nvim_buf_get_lines(0, 0, -1, false), "\n"),
      })
    end,
  })

  menu:mount()

  menu:on(event.BufLeave, function()
    menu:unmount()
  end)
end

return M
