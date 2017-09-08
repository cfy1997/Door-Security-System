
	var database = firebase.database(); // the main reference of the Firebase database


	/** Retrieve the system status, and update the web page content
	 *  The system is "off" means that the system will not detect
	 *  whether there is someone entering your house. 
	 */
    function getSystemStatus()
    {
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/system'; 
			
      var ref =database.ref(userRef); // get the reference address of the "system" status
      ref.on("value", function(snapshot)
      {
        document.getElementById("sstatus").innerHTML=snapshot.val(); // change the web page content
      }, function (errorObject)
          {
          	//failed to read the data
            console.log("The read failed: " + errorObject.code);
          });
    }

    /** The encode command is used to notify the Raspebrrey Pi whether any system status
     *  is changed on the website by users.
     *  When encode command is not 0, it means that some component is changed, and the 
     *  Raspeberry Pi should change the system status.
     *  1: Change the system overall status
     *  2: Request a new image
     *  3: Change the LED status
     *  4: Change the buzzer status
     */
    function sendEncodeCommand(cmd)
		{
			getUserInfo(); // get the user information
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/encodeCommand';
			var ref = database.ref(userRef); // get the reference of the "encodeCommand"
			ref.once('value').then(function(snapshot) {
				ref.set(cmd); // change the value
			});
		}

    /** change the LED status.
     *  First read the LED status, and change it to the opposite.
     *  Also update the web page content, which is done by call getLEDStatus()
     */
	function changeLEDStatus()
	{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/led';
			var ref = database.ref(userRef); // get the reference address of the "led" status
      		ref.once('value').then(function(snapshot) { // only do this once
				const currentStatus = snapshot.val(); // get the current status of the led

				//change the status and update the web page content
				if(currentStatus == "STOPPING")
				{
					ref.set("RUNNING");
					getLEDStatus();
				} else {
					ref.set("STOPPING");
					getLEDStatus();
				}
				sendEncodeCommand(3); // send the encode command
			});
		}


	/** Retrieve the LED status and change the web content
	 */
	function getLEDStatus()
	{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/led';
			var ref = database.ref(userRef); // get the reference of the led status
			ref.on("value", function(snapshot)
  			{
        		document.getElementById("LEDStatus").innerHTML=snapshot.val(); // change the html content based on the data in the database
      		}, function (errorObject)
          		{
          			//failed to read the data
            		console.log("The read failed: " + errorObject.code);
          		});
	}

	/** change the buzzer status 
	 *  First read the buzzer status, and change it to the opposite.
     *  Also update the web page content, which is done by call getBuzzerStatus()
	 */
	function changeBuzzerStatus()
	{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/buzzer';
			var ref = database.ref(userRef); //get the reference address 
      		ref.once('value').then(function(snapshot) {
				const currentStatus = snapshot.val(); // get the current status of the buzzer
				
				//change the status and update the web page content
				if(currentStatus == "RUNNING")
				{
					ref.set("STOPPING");
					getBuzzerStatus();
				} else {
					ref.set("RUNNING");
					getBuzzerStatus();
				}
				sendEncodeCommand(4); send the encode command
			});
	}

	/** retrieve the buzzer status and update the web page content
	 */
	function getBuzzerStatus()
	{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/buzzer';
			var ref = database.ref(userRef); // get the reference address of buzzer status
			ref.on("value", function(snapshot)
      		{
        		document.getElementById("buzzerStatus").innerHTML=snapshot.val(); // change the html content based on the data in the database
     	    }, function (errorObject)
          {
          	//failed to read the data
            console.log("The read failed: " + errorObject.code);
          });
	}

	/** retrieve all the information that overview page needs.
	 *  This inclues:
	 		the system status
	 		user's id
	 		the last login time
	 		number of user that in the current family
	 		work period time
	 		sleep period time
	 *  Then update the web page corresponding content
	 */	
	function getOverviewStatus()
		{
			getUserInfo();
			document.getElementById("overviewuid").innerHTML = document.getElementById("uid").innerHTML;
			document.getElementById("overviewfn").innerHTML = document.getElementById("familyid").innerHTML;
			const family = document.getElementById("familyid").innerHTML;
			const numRef = 'Families/' + family + '/numUsers';
			const userRef = 'Families/' + family + '/info';
			const sysref = 'Families/' + family + '/status/system';
			var ref;

			ref = database.ref(sysref);
			// get the sysetm status
			ref.on("value", function(snapshot)
      		{
				if(snapshot.val()=="off"){
					document.getElementById("lastLoginTime").innerHTML="Your system is currently off";
				} else {
				 //if the system is on, then display the lastLoginTime to user
				  ref = database.ref(userRef + '/lastLoginTime');
				  ref.on("value", function(snapshot)
				  {
						document.getElementById("lastLoginTime").innerHTML= 'Your system has been working since ' + snapshot.val();
				  }, function (errorObject)
					{
						//failed to read the data
						console.log("The read failed: " + errorObject.code);
					});
				}
				updateSystemButtonText(snapshot.val());
      		}, function (errorObject)
          	{
          		//failed to read the data
            	console.log("The read failed: " + errorObject.code);
          	});

			//retrieve work start time value and display on the web page
			ref = database.ref(userRef + '/workstart' );
			ref.on("value", function(snapshot)
		      {
		        document.getElementById("workstart").innerHTML=snapshot.val();
		      }, function (errorObject)
		          {
		            console.log("The read failed: " + errorObject.code);
		 		 });

			//retrieve work end time value and display on the web page
			ref = database.ref(userRef + '/workend' );
			ref.on("value", function(snapshot)
		      {
		        document.getElementById("workend").innerHTML=snapshot.val();
		      }, function (errorObject)
		          {
		            console.log("The read failed: " + errorObject.code);
		  });

			//retrieve sleep start time value and display on the web page
			ref = database.ref(userRef + '/sleepend' );
			ref.on("value", function(snapshot)
					{
						document.getElementById("sleepend").innerHTML=snapshot.val();
					}, function (errorObject)
							{
								console.log("The read failed: " + errorObject.code);
			});

			//retrieve sleep end time value and display on the web page
			ref = database.ref(userRef + '/sleepstart' );
			ref.on("value", function(snapshot)
					{
						document.getElementById("sleepstart").innerHTML=snapshot.val();
					}, function (errorObject)
							{
								console.log("The read failed: " + errorObject.code);
			});

			////retrieve the number of user in the family value and display on the web page
			ref = database.ref(numRef);
			ref.on("value", function(snapshot)
					{
						document.getElementById("memnum").innerHTML=snapshot.val();
					}, function (errorObject)
							{
								console.log("The read failed: " + errorObject.code);
			});

		}

		/** Update the system button text, which shows correct text on the webpage
		 */
		function updateSystemButtonText(status)
		{
			if(status == 'off')
			{
				document.getElementById("systembtm").innerHTML = "Turn on the detecting mode";
			} else {
				document.getElementById("systembtm").innerHTML = "Shut down the detecting mode";
			}
		}

		/** change the system status 
		 *  if trying to turn on the system, then also updatethe lastLoginTime
		 */
		function changeSystemStatus()
		{

			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/status/system';
			var ref = database.ref(userRef); // get the reference address of the system status

			ref.once('value').then(function(snapshot) {
				const currentStatus = snapshot.val(); // get the current status

				//change the system status
				if(currentStatus == "off")
				{
					ref.set("on");
					updateLastLoginTime(); // also update the last login time
				} else {
					ref.set("off");
				}
				sendEncodeCommand(1); // send encode command
			});
		}

		/** get the user information
		 *  this also get the user's family name.
		 */
		function getUserInfo()
		{

			firebase.auth().onAuthStateChanged(function(user) {
  			if (user) 
  			{
				document.getElementById("uid").innerHTML = user.uid; // get the user's uid
				const userid = document.getElementById("uid").innerHTML;
				const userRef = "Users/"+ userid + "/houseName";
				var ref = database.ref(userRef); //get the user's reference address to get the familyid
				ref.on("value", function(snapshot)
	      		{
	        		document.getElementById("familyid").innerHTML=snapshot.val(); // get the user's familyid
	      		}, function (errorObject)
	         		 {
	         		 	//failed to read the data
	            		console.log("The read failed: " + errorObject.code);
	          		});
  				} else {
  				//failed to login
				swal("Error", "You have not signed in!", "error");
				var timer = setTimeout(function() {
					 window.location='../index.html';
			 		}, 1000);
  				}
			});
		}


		/** change the system button text
		 */
		function changesystembtm()
		{
				var text = document.getElementById("systembtm").innerHTML; // get the html text reference 
				var displayText;
				//change following display text
				if(text.toUpperCase() == "TURN ON THE DETECTING MODE")
					displayText = "Are you sure that you want to turn on the detecting mode?";
					else
						displayText = "Are you sure that you want to shut down the detecting mode?";

				// pop-up alert user.
				swal({
					title: "Are you sure?",
					text: displayText,
					type: "warning",
					showCancelButton: true,
					closeOnConfirm: true,
					confirmButtonText: "Yes",
					confirmButtonColor: "#ec6c62"
				}, function() {
					changeSystemStatus();
			});
		}


		/** update the last login time based on the read time.
		 */
		function updateLastLoginTime()
		{
			var date = new Date();
			var year = date.getFullYear();
			var month = date.getMonth()+1;
			var day = date.getDate();
			var hour = date.getHours();
			var minute = date.getMinutes();
			var second = date.getSeconds();
			//get the string that include the time
			const str = year + '-' + month + '-' + day + '- ' + hour + ':' + minute + ':' + second;
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/info/lastLoginTime';
			var ref = database.ref(userRef); //get the reference of the lastlogintime
			//update the last login time in the database
			ref.once('value').then(function(snapshot) {
				const currentStatus = snapshot.val();
				ref.set(str);
			});
		}

		/** sign out the current user, and redirect to the login page.
		 */
		function signout()
		{
			//alarm user to make sure if the user is really want to log out.
			swal({
				title: "Are you sure you want to sign out?",
				text: "(your security system will remain the current state)",
				type: "warning",
				showCancelButton: true,
				confirmButtonColor: "#DD6B55",
				confirmButtonText: "Yes, sign out!",
				closeOnConfirm: false }, function(){
					//display the log out action is successfl
					swal("Sign out Successfully!", "you will be redirect to login page automatically after 3 seconds", "success");
					var timer = setTimeout(function() {
						//delay three seconds and redirect to the index.html
						 window.location='../index.html';
						 firebase.auth().signOut();
					 }, 3000); 
				});

		}
