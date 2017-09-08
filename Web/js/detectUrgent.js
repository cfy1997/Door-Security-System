
  /** This script listen to the "urgent" flag.
   *  When this flag is change to "on", it will show a pop-up alert to users.
   *  This reminds users to go to the gallery to check newly taken images.
   *  When user go to the gallery page, it change the "urgent" flag back to "off"
   */


  firebase.auth().onAuthStateChanged(function(user) {
  if (user) {
// User is signed in.
    const userid = document.getElementById("uid").innerHTML;
    const userRef = "Users/"+ userid + "/houseName";
    var ref = database.ref(userRef);
    ref.on("value", function(snapshot)
    {
      const urgentRef = "Families/" + snapshot.val() + "/urgent";
      var ref = firebase.database().ref(urgentRef) // get the reference address of the "urgent" flag
      ref.on('value', function(snapshot) {
        if(snapshot.val() == "on")
        {
          swal({ // display a pop-up alert
            title: "Cautions!",
            text: "Your system detect something wrong! Please view history gallery since your system has already taken few photos there, you can also take more photos via camera",
            type: "warning",
            showCancelButton: true,
            confirmButtonColor: "#DD6B55",
            confirmButtonText: "Take me to the gallery now",
            cancelButtonText: "remind me later",
            closeOnConfirm: false }, function(){
              ref.set("off"); // change the flag back to "off"
              window.location = 'gallery.html'; // redirect to gallery
            });
        }
      });
    }, function (errorObject)
        {
          // failed to read the "urgent" flag
          console.log("The read failed: " + errorObject.code);
        });
  } else {
    // the user has not signed in
    if(document.getElementById("userSignout").innerHTML == 1)
      return;
    swal("Error", "You have not signed in!", "error");
    var timer = setTimeout(function() {
       window.location='../index.html';
     }, 1000);
    }
  });
