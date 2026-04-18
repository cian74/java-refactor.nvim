Job = require('plenary.job')
local M = {}

-- Get the plugin's root directory dynamically
local function get_plugin_root()
	local str = debug.getinfo(1, "S").source:sub(2)
	local plugin_root = str:match("(.*)/")  -- Get directory of current file
	return vim.fn.fnamemodify(plugin_root, ":h:h")  -- Go up two levels to plugin root
end

local function get_jar_path()
	local plugin_root = get_plugin_root()
	return plugin_root .. "/backend/java-refactor.jar"
end

M.json_buffer = ""

function M.is_backend_running()
	return M.job ~= nil and M.job.is_shutdown == false
end

function M.start_backend()
	if M.is_backend_running() then
		return true
	end

	local jar_path = get_jar_path()
	
	vim.notify("JAR path: " .. jar_path, vim.log.levels.WARN)
	
	if vim.fn.filereadable(jar_path) == 0 then
		vim.notify("Backend JAR not found: " .. jar_path, vim.log.levels.ERROR)
		return false
	end
	
	vim.notify("Starting backend with JAR: " .. jar_path, vim.log.levels.WARN)
	
	M.job = Job:new({
		command = "java",
		args = { "-jar", jar_path },
		on_stdout = function(_, data)
			if not data or data == "" then return end
			vim.schedule(function()
				M.json_buffer = M.json_buffer .. data
				local ok, json_msg = pcall(vim.json.decode, M.json_buffer)
				if not ok then return end
				M.json_buffer = ""

if json_msg.fields then 
					local actions = require("refactor.actions")
					actions.show_field_selection_menu(json_msg.fields)
					return
				end

				if json_msg.flame_graph then
					local ui = require("refactor.ui")
					ui.show_flame_graph(json_msg.flame_graph)
					return
				end

				if json_msg.error and not json_msg.new_source then
					local error_msg = tostring(json_msg.error or "Unknown error")
					vim.notify("Refactoring error: " + error_msg, vim.log.levels.ERROR)
					return
				end
				
				if json_msg.new_source then
					local lines = vim.split(json_msg.new_source, "\n")
					if M.target_buffer and vim.api.nvim_buf_is_valid(M.target_buffer) then
						vim.api.nvim_buf_set_lines(M.target_buffer, 0, -1, false, lines)
					end
					if json_msg.error then
						vim.notify(json_msg.error, vim.log.levels.INFO)
					end
				end

				if json_msg.flame_graph then
					local ui = require("refactor.ui")
					ui.show_flame_graph(json_msg.flame_graph)
					return
				end

				if json_msg.error then
					local error_msg = tostring(json_msg.error or "Unknown error")
					if json_msg.new_source then
						vim.notify(error_msg, vim.log.levels.INFO)
					else
						vim.notify("Refactoring error: " .. error_msg, vim.log.levels.ERROR)
					end
				end
				
				if json_msg.new_source then
					local lines = vim.split(json_msg.new_source, "\n")
					if M.target_buffer and vim.api.nvim_buf_is_valid(M.target_buffer) then
						vim.api.nvim_buf_set_lines(M.target_buffer, 0, -1, false, lines)
					end
				end
			end)
		end,
		on_stderr = function(_, err)
			if err and err ~= "" then
				vim.schedule(function()
					print("[Lua Backend]: " .. err)
				end)
			end
		end,
		on_exit = function(_, code)
			vim.schedule(function()
				M.job = nil
			end)
		end,
	})
	M.job:start()
	-- Don't wait, assume it started
	--vim.notify("Backend started", vim.log.levels.INFO)
	return true
end

function M.send_request(request)
	if not M.is_backend_running() then
		local success = M.start_backend()
		if not success then
			vim.notify("Cannot send request", vim.log.levels.ERROR)
			return
		end
		vim.wait(100)  -- Wait after starting
	end
	M.target_buffer = vim.api.nvim_get_current_buf()
	local ok, json = pcall(vim.json.encode, request)
	if not ok then
		vim.notify("Failed to encode request", vim.log.levels.ERROR)
		return
	end
	local send_ok = pcall(function()
		M.job:send(json .. "\n")
	end)
	if not send_ok then
		vim.notify("Failed to send request", vim.log.levels.ERROR)
		M.job = nil
	end
end

function M.stop_backend()
	if M.job then
		M.job:shutdown()
		M.job = nil
	end
end

return M
