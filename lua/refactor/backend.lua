local Job = require('plenary.job')
local M = {}
local cwd = vim.fn.getcwd()
local jar_path = cwd .. "/backend/target/java-refactor-1.0-SNAPSHOT-jar-with-dependencies.jar"

function M.start_backend()
	M.job = Job:new({
		command = "java",
		args = {"-jar", jar_path },

		on_stdout = function(_, data)
			if data and data ~= "" then
				vim.schedule(function()
					local ok, json_msg = pcall(vim.fn.json_decode,data)
					if ok and json_msg.new_source then
						local lines = {}
						for line in json_msg.new_source:gmatch("[^\r\n]+") do
							table.insert(lines, line)
						end
						vim.api.nvim_buf_set_lines(0, 0, -1, false, lines)
					else
						print("[Java Backend]", data)
					end
				end)
			end
		end,

		on_stderr = function(_, err)
			if err and err ~= "" then
				vim.schedule(function()
					vim.notify("Backend error: " .. err, vim.log.levels.ERROR)
				end)
			end
		end,
	})

	M.job:start()
end

function M.send_request(command, source)
	if not M.job then
		M.start_backend()
	end

	local request = {
		command = command,
		source = source
	}

	local json = vim.json.encode(request) .. "\n"
	M.job:send(json)
end

return M

