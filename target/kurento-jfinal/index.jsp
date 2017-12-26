<html>
<head>
    <title>Welcome Page</title>
</head>
<body>
<h2><a href="javascript: return false;" onclick="redirect('hello_world')">Hello World</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('one2many')">One2Many</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('one2one')">One2One</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('recorder')">Recorder</a></h2>
<h2><a href="javascript: return false;" onclick="redirect('group')">Group</a></h2>
<script>
    function redirect(link) {
        window.location.href = "https://" + location.hostname + "/" + link;
    }
</script>
</body>
</html>
