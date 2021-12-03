$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	// 获取实体id
//	var entityId = $("input#entityId").val();
//    var entityId = document.getElementById("entityId").value;
    var entityId = $(btn).prev().val();
	if($(btn).hasClass("btn-info")) {
		// 关注TA
	    $.post(
            CONTEXT_PATH + "/follow",
            {"entityType":3, "entityId":entityId, "followFlag":0},
            function(data) {
                data = $.parseJSON(data);
                if(data.code == 0){
//                    $(btn).children("i").text(data.followStatus=1?'已关注':'关注TA');
//                    $(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
                    window.location.reload();
                } else {
                    alert(data.message);
                }
            }
        );
	} else {
		// 取消关注
        $.post(
            CONTEXT_PATH + "/follow",
            {"entityType":3, "entityId":entityId, "followFlag":1},
            function(data) {
                data = $.parseJSON(data);
                if(data.code == 0){
//                	$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
                	window.location.reload();
                } else {
                    alert(data.message);
                }
            }
        );
	}
}