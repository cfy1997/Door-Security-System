
/** give suer response when user push the "GIVE ME PHOTO" button	
 */
	function getPhoto()
	{
		sendTakePhotoCommand("true"); // send the take photo command 
		//display the kindly pop-up
		swal("Successful!","It may take a few seconds to retrieve the photo..","success");

	}


	/** display the image to the user
	 */
	function showimage()
	{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/lastPhotoID';
			var ref = database.ref(userRef); // get the reference of the lastPhotoID
			var number;
			ref.on("value", function(snapshot)
      		{
        		number = snapshot.val(); // get the lastPhotoID
				var storageRef = firebase.storage().ref();
				// get the lastPhotoID.png from Firebase storage
				storageRef.child(family + '/images/'+number+'.png').getDownloadURL().then(function(url) {
						var test = url;
						document.getElementById('img').style.display='block';
						document.getElementById('img').src = test;
				}).catch(function(error) {
					//Failed to load the image
				 alert("cannot find the images");
				});
				sendTakePhotoCommand("false"); //change takephotoCommand to "false" to complete the user request 
      		}, function (errorObject)
          {
          	//Failed to read data
            console.log("The read failed: " + errorObject.code);
          });;

	}
	/** hide the photo when button being pressed
	*/
	function hidePhoto()
	{
		document.getElementById('img').style.display='none';
	}

	/** change the takePhotoCommand in the Firebase to setvalue
	 */
	function sendTakePhotoCommand(setvalue)
		{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/takeInstantPhoto';
			var ref = database.ref(userRef); // get the reference address
			ref.once('value').then(function(snapshot) {
				ref.set(setvalue); // set the value
			});
			if(setvalue == "true") 
				sendEncodeCommand(2); // send the encode command
		}

		/** get the Photo id based on the last photoID
		 */
		function getPhotoID()
		{
			getUserInfo();
			const family = document.getElementById("familyid").innerHTML;
			const userRef = 'Families/' + family + '/lastPhotoID';
			const userRef2 = 'Families/' + family + '/deletePhotoNumber';
			var ref = database.ref(userRef);
			var ref2 = database.ref(userRef2);
			var deletenumber = -1;
			ref2.once("value", function(snapshot)
				{
					if(family != "TBD") // if the family is valid
						deletenumber = snapshot.val(); // update the delte photo number
				});
			ref.once("value", function(snapshot)
      {
      	if(document.getElementById("photoCounter").innerHTML == -1 && family != "TBD"){ // if the family is valid
					document.getElementById("photoCounter").innerHTML = snapshot.val(); // the photoCounter is only update once
					if(deletenumber != -1) // 
						document.getElementById("totpho").innerHTML = snapshot.val() - deletenumber;
					refreshPhoto();
				}
      }, function (errorObject)
          {
          	//failed to get the data
            console.log("The read failed: " + errorObject.code);
          });
		}

