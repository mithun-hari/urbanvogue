$ErrorActionPreference = 'Stop'

function Get-Token {
    param([string]$email, [string]$password, [string]$role)
    try {
        $regBody = ConvertTo-Json @{ username = $email.Split('@')[0]; email = $email; password = $password }
        Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/register' -Method Post -Body $regBody -ContentType 'application/json' | Out-Null
        
        if ($role -eq 'ADMIN') {
            cmd /c "mysql -u root -proot@39 -e `"USE urbanvogue_auth; UPDATE users SET role='ADMIN' WHERE email='$email';`""
        }
    } catch {}
    
    $loginBody = ConvertTo-Json @{ email = $email; password = $password }
    $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json'
    return $res.token
}

Write-Host '--- URBANVOGUE E2E TEST ---'
$adminToken = Get-Token 'testadmin1@test.com' 'pass123' 'ADMIN'
$userToken = Get-Token 'testuser1@test.com' 'pass123' 'USER'

Write-Host '[1/5] API Gateway & Auth Service: OK'

try {
    $prodBody = ConvertTo-Json @{ name = 'E2E Test Shoe'; description = 'Running shoe'; price = 129.99; imageUrl = 'http://test.com/shoe.png' }
    $prodRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/products' -Method Post -Body $prodBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $adminToken" }
    $productId = $prodRes.id
    Write-Host "[2/5] Product Service (Admin Create): OK (Prod ID: $productId)"
} catch {
    Write-Host "[2/5] Product Service FAILED: $_"
    exit
}

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/add-stock?productId=$productId&quantity=100" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } | Out-Null
    Write-Host '[3/5] Inventory Service (Admin Add Stock): OK'
} catch {
    Write-Host "[3/5] Inventory Service FAILED: $_"
    exit
}

try {
    $listRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/products' -Method Get -Headers @{ Authorization = "Bearer $userToken" }
    if ($listRes.Length -gt 0) {
        Write-Host '[4/5] Product Service (User Read): OK'
    } else {
        throw 'Empty catalog'
    }
} catch {
    Write-Host "[4/5] Product Search FAILED: $_"
    exit
}

try {
    $orderBody = ConvertTo-Json @{
        items = @( @{ productId = $productId; quantity = 2; price = 129.99 } )
    }
    $orderRes = Invoke-RestMethod -Uri 'http://localhost:8080/orders' -Method Post -Body $orderBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $userToken" }
    $orderId = $orderRes.orderId
    $paymentUrl = $orderRes.paymentUrl
    if ($orderId -and $paymentUrl) {
        Write-Host "[5/5] Order & Payment Service (Checkout): OK (Order ID: $orderId)"
    } else {
        throw "Missing order mapping attributes! Res: $orderRes"
    }
} catch {
    Write-Host "[5/5] Checkout FAILED: $_"
    $stream = $_.Exception.Response.GetResponseStream()
    if ($stream) {
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host $reader.ReadToEnd()
    }
    exit
}

Write-Host '--- ALL SERVICES PASS E2E TEST! ---'
