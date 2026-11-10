/* Rumbo Cargo · interacciones de interfaz. Todo es mejora progresiva: sin JS la página sigue completa. */
(() => {
  const $ = (s, r = document) => [...r.querySelectorAll(s)];
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;

  const editForm = document.querySelector('.edit-user-form');
  if (editForm) {
    const firstName = editForm.querySelector('#firstName');
    const lastName = editForm.querySelector('#lastName');
    const email = editForm.querySelector('#email');
    const previewName = document.querySelector('[data-profile-name]');
    const previewEmail = document.querySelector('[data-profile-email]');
    const previewInitials = document.querySelector('[data-profile-initials]');
    const updatePreview = () => {
      const first = firstName.value.trim();
      const last = lastName.value.trim();
      if (previewName) previewName.textContent = `${first} ${last}`.trim() || 'Nombre completo';
      if (previewEmail) previewEmail.textContent = email.value.trim() || 'Sin correo registrado';
      if (previewInitials) previewInitials.textContent = `${first.charAt(0)}${last.charAt(0)}`.toLocaleUpperCase('es') || 'U';
    };
    editForm.addEventListener('input', updatePreview);
  }

  // Conteo animado de métricas
  if (!reduce) {
    $('.metric-card strong').forEach(el => {
      const txt = el.textContent.trim();
      if (!/^\d+$/.test(txt)) return;
      const n = +txt, t0 = performance.now();
      const tick = t => {
        const p = Math.min(1, (t - t0) / 1000);
        el.textContent = Math.round(n * (1 - Math.pow(1 - p, 3)));
        if (p < 1) requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    });
  }

  // Revelado escalonado al hacer scroll (solo lo que está bajo el pliegue)
  if (!reduce && 'IntersectionObserver' in window) {
    const io = new IntersectionObserver(es => es.forEach(e => {
      if (!e.isIntersecting) return;
      const el = e.target;
      el.classList.add('in');
      el.addEventListener('transitionend', () => el.classList.remove('pre', 'in'), { once: true });
      io.unobserve(el);
    }), { threshold: .12 });
    $('.route-card,.notice-card,.shipment-card,.admin-item,.form-card,.table-card,.metric-card,.request-section,.empty-state').forEach((el, i) => {
      if (el.getBoundingClientRect().top < innerHeight * .92) return;
      el.style.setProperty('--d', i % 4);
      el.classList.add('pre');
      io.observe(el);
    });
  }

  // Mostrar / ocultar contraseña
  $('.auth-page input[type=password]').forEach(input => {
    const wrap = document.createElement('span');
    wrap.className = 'pw-wrap';
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'pw-toggle';
    btn.setAttribute('aria-label', 'Mostrar contraseña');
    btn.setAttribute('aria-pressed', 'false');
    const updateIcon = show => {
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('viewBox', '0 0 24 24');
      svg.setAttribute('width', '20');
      svg.setAttribute('height', '20');
      svg.setAttribute('fill', 'none');
      svg.setAttribute('stroke', 'currentColor');
      svg.setAttribute('stroke-width', '1.8');
      svg.setAttribute('stroke-linecap', 'round');
      svg.setAttribute('stroke-linejoin', 'round');
      svg.setAttribute('aria-hidden', 'true');
      const eye = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      eye.setAttribute('d', 'M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z');
      const pupil = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      pupil.setAttribute('cx', '12');
      pupil.setAttribute('cy', '12');
      pupil.setAttribute('r', '3');
      svg.append(eye, pupil);
      if (show) {
        const slash = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        slash.setAttribute('d', 'm3 3 18 18');
        svg.append(slash);
      }
      btn.replaceChildren(svg);
    };
    updateIcon(false);
    input.replaceWith(wrap);
    wrap.append(input, btn);
    btn.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
      btn.setAttribute('aria-pressed', String(show));
      updateIcon(show);
    });
  });

  // Estado de carga al enviar formularios
  $('.auth-form,.form-card,.inline-status-form').forEach(f => f.addEventListener('submit', e => {
    if (e.defaultPrevented) return;
    const b = f.querySelector('button[type=submit]');
    if (b) setTimeout(() => b.classList.add('is-loading'), 0);
  }));

  // Reloj del panel de administración
  const clock = document.querySelector('[data-clock]');
  if (clock) {
    const upd = () => clock.textContent = new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
    upd();
    setInterval(upd, 1000);
  }

  // Parallax suave del escenario de acceso
  const stage = document.querySelector('.stage');
  if (stage && !reduce) {
    addEventListener('pointermove', e => {
      stage.style.setProperty('--mx', (e.clientX / innerWidth - .5).toFixed(3));
      stage.style.setProperty('--my', (e.clientY / innerHeight - .5).toFixed(3));
    }, { passive: true });
  }

  // Sección activa en la navegación
  const links = $('.main-nav a[href^="#"]');
  if (links.length && 'IntersectionObserver' in window) {
    const spy = new IntersectionObserver(es => es.forEach(e => {
      if (!e.isIntersecting) return;
      links.forEach(a => a.toggleAttribute('aria-current', a.getAttribute('href') === '#' + e.target.id));
    }), { rootMargin: '-40% 0px -55% 0px' });
    links.forEach(a => {
      const t = document.querySelector(a.getAttribute('href'));
      if (t) spy.observe(t);
    });
  }

  // Búsqueda progresiva en tablas administrativas
  const tableFilter = document.querySelector('[data-table-filter]');
  if (tableFilter) {
    const rows = $('[data-filter-row]');
    const emptyRow = document.querySelector('[data-filter-empty]');
    tableFilter.addEventListener('input', () => {
      const query = tableFilter.value.trim().toLocaleLowerCase('es');
      let visibleRows = 0;
      rows.forEach(row => {
        const matches = row.dataset.searchText.toLocaleLowerCase('es').includes(query);
        row.hidden = !matches;
        if (matches) visibleRows++;
      });
      if (emptyRow) emptyRow.hidden = query.length === 0 || visibleRows > 0;
    });
  }
})();
