lucide.createIcons({ attrs: { 'stroke-width': 2.5 } });
localStorage.removeItem('returnUrl');
setTimeout(() => {
    window.location.href = redirectUrl;
}, 3000);