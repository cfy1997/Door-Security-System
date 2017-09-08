
  /** This script is used to detect the "takeInstantPhoto" change.
   *  When the flag is changed to "pending", this means that the image
   *  is store in the storage already and ready to display to the user.
   *  Then it retrieve the images, and display to the user.
   *
   */
  
  firebase.auth().onAuthStateChanged(function(user) {
  if (user) {
    // User is signed in.
    const userid = document.getElementById("uid").innerHTML;
    const userRef = "Users/"+ userid + "/houseName";
    var ref = database.ref(userRef);
    ref.on("value", function(snapshot) 
    {
      const urgentRef = "Families/" + snapshot.val() + "/takeInstantPhoto"; // get the reference address of the flag
      var ref = firebase.database().ref(urgentRef)
      ref.on('value', function(snapshot) {
        if(snapshot.val() == "pending")
        {
          swal("photo arrived!"); // display a message to the user
          showimage(); 
        }
      });
    }, function (errorObject)
        {
          //cannot read the image
          console.log("The read failed: " + errorObject.code);
        });
  } else {
    // if the user has not sign in
    if(document.getElementById("userSignout").innerHTML == 1)
      return;
    swal("Error", "You have not signed in!", "error");
    var timer = setTimeout(function() {
       window.location='../index.html';
     }, 1000);
    }
  });
