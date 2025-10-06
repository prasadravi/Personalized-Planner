// Nice, small stepper + subtle UI interactions
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('plannerForm');
  const steps = Array.from(document.querySelectorAll('.form-step'));
  const stepperNodes = Array.from(document.querySelectorAll('.step'));
  let current = 0;

  function showStep(idx){
    steps.forEach((s,i)=>{
      s.classList.toggle('active', i===idx);
      s.setAttribute('aria-hidden', i===idx ? 'false' : 'true');
    });
    stepperNodes.forEach((n,i)=> n.classList.toggle('active', i===idx));
    // smooth scroll card top on mobile
    window.scrollTo({ top: document.querySelector('.card').offsetTop - 12, behavior: 'smooth' });
  }

  // next buttons
  document.querySelectorAll('[data-next]').forEach(btn=>{
    btn.addEventListener('click', ()=>{
      if(current < steps.length - 1) {
        // basic validation for required fields in current step
        const inputs = steps[current].querySelectorAll('input,select');
        for(let el of inputs){
          if(el.hasAttribute('required') && !el.value){
            el.classList.add('shake');
            setTimeout(()=>el.classList.remove('shake'),400);
            el.focus();
            return;
          }
        }
        current++;
        showStep(current);
      }
    });
  });

  // prev buttons
  document.querySelectorAll('[data-prev]').forEach(btn=>{
    btn.addEventListener('click', ()=>{
      if(current > 0){ current--; showStep(current); }
    });
  });

  // floating label support for selects
  document.querySelectorAll('.field select').forEach(s=>{
    s.addEventListener('change', ()=> {
      if(s.value) s.classList.add('has-value'); else s.classList.remove('has-value');
    });
  });

  // subtle focus glow
  document.querySelectorAll('.field input, .field select').forEach(el=>{
    el.addEventListener('focus', ()=> el.closest('.field').classList.add('focus'));
    el.addEventListener('blur', ()=> el.closest('.field').classList.remove('focus'));
  });

  // form submit animation
  form.addEventListener('submit', (e)=>{
    const submit = form.querySelector('button[type="submit"]');
    submit.disabled = true;
    submit.innerText = 'Generating...';
    // allow normal post to server
  });

  // initial
  showStep(0);
});
