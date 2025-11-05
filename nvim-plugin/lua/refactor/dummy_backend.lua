local M = {}

function M.start_backend()
  -- do nothing
end

function M.send_request(request)
  -- just print the request safely
  vim.schedule(function()
    vim.notify("Pretending to send request: " .. vim.inspect(request))
  end)
end

return M

