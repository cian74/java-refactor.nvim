local Split = require("nui.split")
local M = {}

local ai_panel = nil

local function is_java_file()
    return vim.bo.filetype == "java"
end

function M.ai_find_usages()
    if not is_java_file() then
        vim.notify("Not a Java file", vim.log.levels.WARN)
        return
    end

    -- Get word under cursor or selection
    local word = vim.fn.expand("<cword>")
    if not word or word == "" then
        vim.notify("No word under cursor", vim.log.levels.WARN)
        return
    end

    show_panel()
    update_panel("Searching for: " .. word .. "...")

    -- Use vim.system with table format
    local cmd = { "opencode", "run", "Grep " .. word }
    
    vim.system(cmd, { text = true }, function(obj)
        vim.schedule(function()
            if obj.code == 0 then
                local text = obj.stdout
                text = text:gsub("\27%[%dm", "")
                text = text:gsub("\27%[90m", "")
                update_panel(text .. "\n[Done. Press q or Esc to close]")
            else
                update_panel("Error: " .. (obj.stderr or "failed") .. "\n[Done. Press q or Esc to close]")
            end
        end)
    end)
end

function show_panel()
    if ai_panel then
        pcall(function() ai_panel:unmount() end)
        ai_panel = nil
    end

    ai_panel = Split({
        position = "bottom",
        size = 15,
        border = {
            style = "rounded",
            text = { top = " AI Search ", top_align = "center" },
        },
        win_options = { winhighlight = "Normal:Normal,FloatBorder:Normal" },
    })

    ai_panel:mount()
    vim.bo[ai_panel.bufnr].filetype = "markdown"
    vim.bo[ai_panel.bufnr].readonly = false

    vim.keymap.set("n", "q", function() close_panel() end, { buffer = ai_panel.bufnr, nowait = true })
    vim.keymap.set("n", "<Esc>", function() close_panel() end, { buffer = ai_panel.bufnr, nowait = true })
end

function update_panel(text)
    if not ai_panel or not ai_panel.bufnr then return end
    if not vim.api.nvim_buf_is_valid(ai_panel.bufnr) then return end

    local lines = vim.split(text, "\n", { plain = true })
    if #lines > 0 and lines[1] == "" then
        lines[1] = "Searching..."
    end

    vim.api.nvim_buf_set_lines(ai_panel.bufnr, 0, -1, false, lines)
end

function close_panel()
    if ai_panel then
        pcall(function() ai_panel:unmount() end)
        ai_panel = nil
    end
end

return M