local backend = require("refactor.backend")
local M = {}

function M.generate_getters_setters()
	-- Get current buffer content
	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, 0, -1, false)
	local source = table.concat(lines, "\n")
	-- Send request (as a table!)
	backend.send_request({
		command = "generate_getters_setters",
		source = source
	})
end

function M.extract_method()
	local start_line = vim.fn.line("'<") - 1
	local end_line = vim.fn.line("'>") - 1
	local start_col = vim.fn.col("'<") - 1
	local end_col = vim.fn.col("'>")

	local highlighted

	local buf = vim.api.nvim_get_current_buf()
	local lines = vim.api.nvim_buf_get_lines(buf, start_line, end_line + 1, false)

	--handling single line selection
	if start_line == end_line then
		highlighted = string.sub(lines[1], start_col + 1, end_col)
	else
		lines[1] = string.sub(lines[1],start_col + 1)
		lines[#lines] = string.sub(lines[#lines],1,end_col)
		highlighted = table.concat(lines, "\n")
	end

	--print("Highlighted text: '" .. highlighted .. "'")
	--print("==================")

	local current_lines = vim.api.nvim_buf_get_lines(buf,0,-1,false)
	local current_source = table.concat(current_lines, "\n")

	vim.ui.input({
		prompt = 'Enter Method name: ',
		default = 'extractedMethod'
	}, function(method_name)
		if not method_name or method_name == "" then
			vim.notify("Cancelled",vim.log.levels.INFO)
			return
		end
		backend.send_request({
			command = "extract_method",
			source = current_source,
			start_line = start_line + 1,
			end_line = end_line + 1,
			method_name = method_name,
			highlighted = highlighted
		})
	end)
end



return M
