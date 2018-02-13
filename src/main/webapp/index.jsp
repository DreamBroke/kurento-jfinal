<html>
<head>
    <title>Welcome Page</title>
</head>
<body>
<h2><a href="javascript: return false;" onclick="redirect('hello-world')">HelloWorld</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('one2many')">One2Many</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('one2one')">One2One</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('recorder')">Recorder</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('group')">Group</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('screen-sharing')">ScreenSharing</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('mixing')">Mixing</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('broadcast')">Broadcast</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('file-save')">FileSave</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('broadcast-http')">BroadcastHttp</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('broadcast-js')">BroadcastJs</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('happy-new-year/presenter')">HappyNewYear-presenter</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('happy-new-year/viewers')">HappyNewYear-viewers</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('ios')">IOS</a></h2>

<button onclick="test()">Test</button>
<script src="https://cdn.bootcss.com/jquery/3.2.1/jquery.min.js"></script>
<script>
    function redirect(link) {
        window.location.href = "https://" + location.hostname + "/" + link;
    }

    function test() {
        $.ajax({
            type: "put",
            url: "http://192.168.1.143:8080/file-save/mp4.mp4",
            data: {
                username: "xxx"
            },
            dataType: "json"
        });
    }
</script>
</body>
</html>
