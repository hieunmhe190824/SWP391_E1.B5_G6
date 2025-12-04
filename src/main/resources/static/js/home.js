const toggleButton = document.getElementById('toggle-btn')
const sidebar = document.getElementById('sidebar')


function toggleSidebar(){
    sidebar.classList.toggle('transition')
    sidebar.classList.toggle('close')

    Array.from(sidebar.getElementsByClassName('show')).forEach(ul => {
        ul.classList.remove('show')
        ul.previousElementSibling.classList.remove('rotate')
    })
}

function toggleSubmenu(button){
    button.nextElementSibling.classList.toggle('show')
    button.classList.toggle('rotate')
    if(sidebar.classList.contains('close')){
        sidebar.classList.toggle('transition')
        sidebar.classList.toggle('close')
        toggleButton.classList.toggle('rotate')
    }
}

if (window.performance && window.performance.getEntriesByType) {
  const navigationEntries = window.performance.getEntriesByType('navigation');

  if (navigationEntries.length > 0) {
    const navigationType = navigationEntries[0].type;

    // if (navigationType === 'reload') {
    //   console.log('This page was reloaded.');
    //   // Perform actions specific to a page refresh
    // } else if (navigationType === 'navigate') {
    //   console.log('This is a fresh navigation.');
    // } else if (navigationType === 'back_forward') {
    //   console.log('This page was accessed via back/forward button.');
    // } else if (navigationType === 'prerender') {
    //   console.log('This page was prerendered.');
    // }
    // if(navigationType === 'navigate' && !sidebar.classList.contains('close')){
    //     sidebar.classList.toggle('close')
    //     console.log('worked')
    // } 
  }
} else {
  console.log('PerformanceNavigationTiming API is not supported in this browser.');
  // Fallback or alternative logic for older browsers
}