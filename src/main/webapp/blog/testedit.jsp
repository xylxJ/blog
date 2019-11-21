<html lang="en">
    <head>
        <meta charset="utf-8">
        <title>A Simple Page with CKEditor</title>
        <!-- Make sure the path to CKEditor is correct. -->
        <script src="/ckeditor/ckeditor.js"></script>
    </head>
    <body>
        <form>
            <textarea name="editor1" id="editor1" rows="10" cols="80">
                This is my textarea to be replaced with CKEditor.
            </textarea>
            <script>
                CKEDITOR.replace( 'editor1' , {
                	 filebrowserImageUploadUrl: "imgupload.do",
                	 language : 'zh-cn',
                } );
                
            </script>
        </form>
    </body>
</html>