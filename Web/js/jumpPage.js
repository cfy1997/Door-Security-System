/** jump to specific page according to the button's id
 */
function jumpPage(e)
{
  var id = e.id;
  switch (id) {
    case "home":
      window.location = 'home.html';
      break;
    case "camera":
      window.location = 'camera.html';
      break;
    case "about_us":
      window.location = 'about_us.html';
      break;
    case "buzzer":
      window.location = 'buzzer.html';
      break;
    case "led":
      window.location = 'led.html';
      break;
    case "overview":
      window.location = 'overview.html';
      break;
    case "gallery":
      window.location = 'gallery.html';
      break;
    default:
  }
}
