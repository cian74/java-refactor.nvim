local backend = require("refactor.backend")
local M = {}

function M.generate_getters_setters()
    -- Get current buffer content
    local buf = vim.api.nvim_get_current_buf()
    local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
    local source = table.concat(lines, "\n")
    print("=== Sending refactor request ===")
    print("Source: " .. source)
    -- Send request (as a table!)
    backend.send_request({
        command = "generate_getters_setters",
        source = source
    })
end

return M
