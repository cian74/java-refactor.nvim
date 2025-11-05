local Job = require('plenary.job')
local M = {}

function M.start_backend()
  M.job = Job:new({
    command = "java",
    args = {"-jar", "/home/cian/Work/java-refactor-tool/backend/target/java-refactor-1.0-SNAPSHOT.jar"},

    on_stdout = function(_, data)
      if data and data ~= "" then
        vim.schedule(function()
          print("[Java Backend]", data)
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

function M.send_request(request)
  if not M.job then
    vim.schedule(function()
      vim.notify("Backend not running", vim.log.levels.ERROR)
    end)
    return
  end

  local json = vim.fn.json_encode(request)
  M.job:send(json .. "\n")
end

return M

