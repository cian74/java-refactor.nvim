local Job = require('plenary.job')
local M = {}

local cwd = vim.fn.getcwd()
local jar_path = cwd .. "/backend/target/java-refactor-1.0-SNAPSHOT-jar-with-dependencies.jar"

-- Buffer for streaming JSON (backend may output in chunks)
M.json_buffer = ""

function M.start_backend()
<<<<<<< HEAD
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
=======
    if M.job then
        print("Backend already running")
        return
    end

    print("Starting backend at: " .. jar_path)

    M.job = Job:new({
        command = "java",
        args = { "-jar", jar_path },

        on_stdout = function(_, data)
            if not data or data == "" then return end

            print("[STDOUT RAW]:", vim.inspect(data))

            -- Append streamed output
            M.json_buffer = M.json_buffer .. data

            -- Try decode the buffer
            local ok, json_msg = pcall(vim.json.decode, M.json_buffer)

            if not ok then
                print("[JSON not complete yet]")
                return
            end

            -- JSON parse succeeded
            print("[JSON DECODE SUCCESS]:", vim.inspect(json_msg))

            -- Clear buffer after successful parse
            M.json_buffer = ""

            if json_msg.new_source then
                print("new_source received, applying to buffer")

                local lines = {}
                for line in json_msg.new_source:gmatch("[^\r\n]+") do
                    table.insert(lines, line)
                end

                local bufnr = vim.api.nvim_get_current_buf()
                print("Applying to buffer number:", bufnr)

                vim.api.nvim_buf_set_lines(bufnr, 0, -1, false, lines)
            else
                print("[new_source missing in JSON message]")
            end
        end,

        on_stderr = function(_, err)
            if err and err ~= "" then
                print("[STDERR]:", err)
            end
        end,

        on_exit = function(_, code)
            print("Backend exited with code:", code)
            M.job = nil
        end,
    })

    M.job:start()
    print("Backend started")
end

function M.send_request(request)
    if not M.job then
        print("Backend not running, starting...")
        M.start_backend()
    end

    print("========== SENDING REQUEST ==========")
    print("Command:", request.command)
    print("Source:", request.source)
    print("=====================================")

    local json = vim.json.encode(request) .. "\n"
    print("JSON sent to backend:", json)

    M.job:send(json)
    print("Request dispatched")
end

function M.stop_backend()
    if M.job then
        print("Stopping backend...")
        M.job:shutdown()
        M.job = nil
        print("Backend stopped")
    end
>>>>>>> 25418c6 (writing to buffer and logging messages)
end

return M

