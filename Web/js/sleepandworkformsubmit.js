
function isTime(form) {
  // regular expression to match required time format
  re = /^\d{1,2}:\d{2}([ap]m)?$/;

  if(form.workstart.value != '' && !form.starttime.value.match(re)) {
    alert("Invalid time format: " + form.starttime.value);
    form.starttime.focus();
    return false;
}

alert("All input fields have been validated!");
return true;
   }

document.getElementById('workform').addEventListener('submit', function(evt)
  {
    evt.preventDefault();
    getUserInfo();
    var start = document.getElementById("workstartform").value;
    var end = document.getElementById("workendform").value;
    //alert(isTime(document.getElementById("workform")));
    const family = document.getElementById("familyid").innerHTML;
    const startRef = 'Families/' + family + '/info/workstart';
    const endRef = 'Families/' + family + '/info/workend';
    var ref1 = database.ref(startRef);
    var ref2 = database.ref(endRef);
    ref1.once('value').then(function(snapshot) {
      ref1.set(start);
    });
    ref2.once('value').then(function(snapshot) {
      ref2.set(end);
    });
  });

  document.getElementById('sleepform').addEventListener('submit', function(evt)
    {
      evt.preventDefault();
      getUserInfo();
      var start = document.getElementById("sleepstartform").value;
      var end = document.getElementById("sleependform").value;
      const family = document.getElementById("familyid").innerHTML;
      const startRef = 'Families/' + family + '/info/sleepstart';
      const endRef = 'Families/' + family + '/info/sleepend';
      var ref1 = database.ref(startRef);
      var ref2 = database.ref(endRef);
      ref1.once('value').then(function(snapshot) {
        ref1.set(start);
      });
      ref2.once('value').then(function(snapshot) {
        ref2.set(end);
      });
    });
