$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	$("#sendModal").modal("hide");

		// 获取标题和内容
    	var toUsername = $("#recipient-name").val();
    	var content = $("#message-text").val();
    	// 发送异步请求
    	$.post(
    	    CONTEXT_PATH + "/message/send",
    	    {"toUsername":toUsername,"content":content},
    	    function(data) {
    	        data = $.parseJSON(data);
    	        console.log(data);
    	        if (data.code == 0) {
                    $("hintBody").text("发送成功");
    	        } else {
                    // 在提示框中显示返回信息
                    $("hintBody").text(data.message);
    	        }

                $("#hintModal").modal("show");
                setTimeout(function(){
                    $("#hintModal").modal("hide");
                    window.location.reload();
                }, 2000);
    	    }
    	)
}

function delete_msg() {
	// TODO 删除数据

	$(this).parents(".media").remove();
}