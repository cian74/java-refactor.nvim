local Job = require('plenary.job')
local M = {}

local cwd = vim.fn.getcwd()
local jar_path = cwd .. "/backend/target/java-refactor-1.0-SNAPSHOT-jar-with-dependencies.jar"

M.json_buffer = ""

function M.is_backend_running()
    return M.job ~= nil and M.job.is_shutdown == false
end

function M.start_backend()
    if M.is_backend_running() then
        return true
    end

    if vim.fn.filereadable(jar_path) == 0 then
        vim.notify("Backend JAR not found", vim.log.levels.ERROR)
        return false
    end

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

                if json_msg.new_source then
                    local lines = vim.split(json_msg.new_source, "\n")

                    if M.target_buffer and vim.api.nvim_buf_is_valid(M.target_buffer) then
                        vim.api.nvim_buf_set_lines(M.target_buffer, 0, -1, false, lines)
                        vim.notify("Refactoring applied", vim.log.levels.INFO)
                    end
                end
            end)
        end,
        on_stderr = function(_, err)
            if err and err ~= "" then
                vim.schedule(function()
                    print("[Backend]: " .. err)
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
    vim.notify("Backend started", vim.log.levels.INFO)
    return true
end

function M.send_request(request)
    if not M.is_backend_running() then
        local success = M.start_backend()
        if not success then
            vim.notify("Cannot send request", vim.log.levels.ERROR)  -- Fixed typo
            return
        end
        vim.wait(1000)  -- Wait after starting
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
