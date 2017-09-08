
  /** get the photo and display in the corresponding area
   *   photo_num: the photo ID that tries to read from the storage
   */
  function getOnePhoto(photo_num, i, timess)
  {
    if(photo_num <= 0) return 0; // if no images in the storage then return
    getUserInfo();
    var storageRef = firebase.storage().ref();
    const family = document.getElementById("familyid").innerHTML;

    //get the reference of the photo_num images, and try to get this photo
    storageRef.child( family + '/images/' + photo_num + '.png').getDownloadURL().then(function(url) {
      if(timess < i-1)
        getOnePhoto(photo_num-1,i,timess+1);
      else{
        var test = url;
        var id = 'photo' + i;
        var desc = 'desc' + i;
        var ref = 'ref' + i;
        document.getElementById(id).style.display='block';
        document.getElementById(id).src = test;
        document.getElementById(desc).innerHTML='PhotoID: ' + photo_num +'.png';
        document.getElementById(ref).href=test;
        if(i==4)
          document.getElementById("lastPagePhoto").innerHTML = photo_num;
      }
    }).catch(function(error) {
      //if this photo is not exist (probobaly deleted by the user), then try to get the next image
     return getOnePhoto(photo_num-1,i,timess);
    });
    return photo_num; // return the successful photo id
  }


  /** refresh the four photos on the gallery page 
   */
  function refreshPhoto()
  {
    getUserInfo();
    var storageRef = firebase.storage().ref();
    var current_photo_num = document.getElementById("photoCounter").innerHTML;
    const family = document.getElementById("familyid").innerHTML;
    getOnePhoto(current_photo_num,1,0);//update the left-top photo
    getOnePhoto(current_photo_num,2,0);//update the right-top photo
    getOnePhoto(current_photo_num,3,0);//update the left-bottom photo
    getOnePhoto(current_photo_num,4,0);//update the right-bottom photo
  }

  /** refresh the photo counter, and refresh four photos.
   *  This function is different from the refershPhoto().
   *  This function is triggered when the "refresh" button is pressed.
   *  And it shows the latest four images even if the user turn to older pages
   */
  function newrefreshPhoto()
  {
    
    getUserInfo();
    const family = document.getElementById("familyid").innerHTML;
    const userRef = 'Families/' + family + '/lastPhotoID';
    var ref = database.ref(userRef); // get the reference of the latsPhotoID
    ref.on("value", function(snapshot)
    {
      document.getElementById("photoCounter").innerHTML=snapshot.val(); // update the photo counter to be equal to the latest last photo ID
      var storageRef = firebase.storage().ref();
      var current_photo_num = document.getElementById("photoCounter").innerHTML;
      const family = document.getElementById("familyid").innerHTML;
      //update four photos displayed on the web page
      getOnePhoto(current_photo_num,1,0);
      getOnePhoto(current_photo_num,2,0);
      getOnePhoto(current_photo_num,3,0);
      getOnePhoto(current_photo_num,4,0);
    }, function (errorObject)
        {
          //failed to read the data
          console.log("The read failed: " + errorObject.code);
        });
  }

  /** Delete any image that the user request
   */
  function deletePhoto(galleryid)
  {
    getUserInfo();
    var storageRef = firebase.storage().ref();
    var display = document.getElementById("desc"+ galleryid).innerHTML;
    display = display.substr(9); // get the image file name

    const family = document.getElementById("familyid").innerHTML;
    var desertRef = storageRef.child(family+'/images/'+display);
    desertRef.delete().then(function() {
        // File deleted successfully
        swal("Deleted!", "Your photo has been deleted.", "success");
        document.getElementById("totpho").innerHTML--;
        const family = document.getElementById("familyid").innerHTML;
        const userRef = 'Families/' + family + '/deletePhotoNumber';
        var ref = database.ref(userRef);
        var deletenumber = -1;
        ref.once("value", function(snapshot)
          {
              deletenumber = snapshot.val();
              ref.set(deletenumber+1);
          });
        refreshPhoto();
      });
  }

  /** get more recent photos and display them on the websites
   */
  function leftbuttonAction() {
    document.getElementById("photoCounter").innerHTML = document.getElementById("photoCounter").innerHTML - (-4); // add 4 to the photo counter so it display more recent photos
    refreshPhoto();
  }

  /** get more ancient phots and display them on the websites
   */
  function rightbuttonAction() {
    var current = document.getElementById("photoCounter").innerHTML;
    if(document.getElementById("lastPagePhoto").innerHTML > 1)
    {
      document.getElementById("photoCounter").innerHTML = document.getElementById("lastPagePhoto").innerHTML - 1;
      refreshPhoto();
    }
  }


/** display the confirm message to make sure user misuse
 */
function deleteConfirm(id) {
  swal({
    title: "Are you sure?",
    text: "You will not be able to recover this photo!",
    type: "warning",
    showCancelButton: true,
    confirmButtonColor: "#DD6B55",
    confirmButtonText: "Yes, delete it!",
    closeOnConfirm: false },
    function(){
      deletePhoto(id);
    });
}
