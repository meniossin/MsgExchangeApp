<html>
<body>
    <h1>Simple message service with RabbitMQ</h1>
    <form id="form" action="send" method="POST">
        <label for="name">Name</label>
        <input type="text" name="fullName" id="name1" value="">
        <label for="phone">Phone</label>
        <input type="text" name="phone" id="phone1" value="">
        <br/><br/>
        <input id="submit"  type="button" value="Submit">
    </form>
    <div id ="msg"> </div>
    <div id="loadingDiv">Loading....</div>

     <script src="js/jquery-3.5.1.min.js"></script>
     <script>
	   var $loading = $('#loadingDiv').hide();
	   $(document)
		.ajaxStart(function () {
		   $loading.show();
		})
		.ajaxStop(function () {
		   $loading.hide();
		});

		 $(document).ready(function(){
			 // click on button submit
			 $("#submit").on('click', function(){

				var data = {
					fullName:  $("#name1").val(),
					phone: $("#phone1").val()
				};
				 console.log("Sending data:" + JSON.stringify(data));
				 // send ajax
				 $.ajax({
					 url: 'send',
					 type : "POST",
					 contentType: 'application/json; charset=utf-8',
					 data : JSON.stringify(data) ,
					 success : function(result) {
						 console.log("Success: " + JSON.stringify(result));

						 if(result.user.firstName != null && result.user.lastName != null){
							var name = JSON.stringify(result.user.firstName) + " " + JSON.stringify(result.user.lastName);
						 }
						 else{
							var name = JSON.stringify(result.user.fullName);
						 }

						 var msg = "User " +  name + " with phone " + JSON.stringify(result.user.phone) + " has just signed up!";
						 $('#msg').html(msg).fadeIn('slow');
					 },
					 error: function(xhr, resp, text) {
						console.log("Error Response text:" + xhr.responseText);
							$('#msg').html(xhr.responseText).fadeIn('slow');
					 }
				 })
			 });
         });

     </script>
</body>
</html>
