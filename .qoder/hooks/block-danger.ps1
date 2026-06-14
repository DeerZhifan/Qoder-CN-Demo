$raw = [Console]::In.ReadToEnd()
$evt = $raw | ConvertFrom-Json
$cmd = $evt.tool_input.command
# 拦：删库/清表/递归删除/强推
if ($cmd -match 'rm\s+-rf|DROP\s+TABLE|TRUNCATE|DELETE\s+FROM\s+kb_\w+(?!.*WHERE)|git\s+push\s+.*--force') {
    [Console]::Error.WriteLine("危险命令已拦截: $cmd")
    exit 2
}
exit 0