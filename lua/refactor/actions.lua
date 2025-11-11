local backend = require("refactor.backend")

local M = {}

function M.generate_getters_setters()
	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines,"\n")

	backend.send_request( "generate_getters_setters", source)
end

return M
