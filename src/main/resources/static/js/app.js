(function () {
  // ---- Login page ----
  if (document.getElementById('login-form')) {
    const form = document.getElementById('login-form');
    const submitBtn = document.getElementById('login-submit');
    const errorEl = document.getElementById('login-error');
    const toggleBtn = document.getElementById('toggle-password');
    const passwordInput = document.getElementById('password');
    const iconEye = document.getElementById('icon-eye');
    const iconEyeOff = document.getElementById('icon-eye-off');
  
    let showPassword = false;
  
    toggleBtn.addEventListener('click', function () {
      showPassword = !showPassword;
      passwordInput.type = showPassword ? 'text' : 'password';
      iconEye.style.display = showPassword ? 'none' : '';
      iconEyeOff.style.display = showPassword ? '' : 'none';
      toggleBtn.setAttribute('aria-label', showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
  
    function setLoading(isLoading) {
      submitBtn.disabled = isLoading;
      submitBtn.textContent = isLoading ? 'Iniciando sesión…' : 'Iniciar sesión';
    }
  
    form.addEventListener('submit', async function (e) {
      e.preventDefault();
      const username = form.username.value;
      const password = form.password.value;
  
      errorEl.classList.remove('visible');
      setLoading(true);
  
      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password })
        });
  
        if (res.ok) {
          window.location.href = 'index.html';
          return;
        }
        errorEl.classList.add('visible');
      } catch (err) {
        errorEl.classList.add('visible');
      } finally {
        setLoading(false);
      }
    });
  
  }

  // ---- Dashboard page ----
  if (document.getElementById('csr-form')) {
    const usernameSlot = document.getElementById('username-slot');
    const csrForm = document.getElementById('csr-form');
    const sanList = document.getElementById('san-list');
    const addSanBtn = document.getElementById('add-san');
    const keysizeField = document.getElementById('keysize-field');
    const curveField = document.getElementById('curve-field');
    const formError = document.getElementById('form-error');
    const generateBtn = document.getElementById('generate-btn');
    const resultPanel = document.getElementById('result-panel');
    const downloadCsr = document.getElementById('download-csr');
    const downloadKey = document.getElementById('download-key');
    const historyTable = document.getElementById('history-table');
    const historyBody = document.getElementById('history-body');
    const emptyHistory = document.getElementById('empty-history');
    const logoutBtn = document.getElementById('logout-btn');
    const pwInput = document.getElementById('pw');
    const pwStrengthEl = document.getElementById('pw-strength');
  
    // ---- Session check + history load ----
    fetch('/api/auth/session').then(function (res) {
      if (res.status === 401) { window.location.href = 'login.html'; return null; }
      return res.json().catch(function () { return null; });
    }).then(function (data) {
      if (data && data.username) usernameSlot.textContent = data.username;
    }).catch(function () {});
  
    function loadHistory() {
      fetch('/api/csr/historial').then(function (res) {
        return res.ok ? res.json() : [];
      }).then(function (list) {
        renderHistory(Array.isArray(list) ? list : []);
      }).catch(function () {
        renderHistory([]);
      });
    }
    
    loadHistory();
  
    function renderHistory(rows) {
      if (!rows.length) {
        historyTable.style.display = 'none';
        emptyHistory.style.display = '';
        return;
      }
      historyBody.innerHTML = '';
      rows.forEach(function (row) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td></td><td></td><td></td><td></td>';
        tr.children[0].textContent = row.fecha || '';
        tr.children[1].textContent = row.cn || '';
        tr.children[2].textContent = row.algo || '';
        tr.children[3].textContent = row.org || '';
        historyBody.appendChild(tr);
      });
      historyTable.style.display = '';
      emptyHistory.style.display = 'none';
    }
  
    // ---- Logout button ----
    logoutBtn.addEventListener('click', async function () {
      try {
        await fetch('/api/auth/logout', { method: 'POST' });
      } catch (err) {
        console.error('Logout error:', err);
      } finally {
        window.location.href = 'login.html';
      }
    });
  
    // ---- SAN dynamic list ----
    function addSanRow(value) {
      const row = document.createElement('div');
      row.className = 'san-row';
      row.innerHTML =
        '<input class="input" placeholder="ejemplo.com o 10.0.0.1" value="' + (value || '') + '">' +
        '<button type="button" class="btn btn-ghost btn-icon" aria-label="Quitar">×</button>';
      row.querySelector('button').addEventListener('click', function () {
        if (sanList.children.length > 1) {
          row.remove();
        } else {
          row.querySelector('input').value = '';
        }
      });
      sanList.appendChild(row);
    }
    addSanRow('');
    addSanBtn.addEventListener('click', function () { addSanRow(''); });
  
    // ---- Key type toggle ----
    Array.prototype.forEach.call(document.getElementsByName('keytype'), function (radio) {
      radio.addEventListener('change', function () {
        if (radio.checked) {
          keysizeField.style.display = radio.value === 'RSA' ? '' : 'none';
          curveField.style.display = radio.value === 'EC' ? '' : 'none';
        }
      });
    });
  
    // ---- Password strength indicator ----
    pwInput.addEventListener('input', function () {
      const pw = pwInput.value;
      let strength = 0;
      let message = '';
      
      if (pw.length >= 8) strength += 1;
      if (pw.length >= 12) strength += 1;
      if (/[A-Z]/.test(pw)) strength += 1;
      if (/[0-9]/.test(pw)) strength += 1;
      if (/[!@#$%^&*]/.test(pw)) strength += 1;
      
      if (pw.length === 0) {
        message = '';
      } else if (strength <= 2) {
        message = '⚠️ Contraseña débil';
        pwStrengthEl.style.color = 'var(--color-accent-2-700)';
      } else if (strength <= 3) {
        message = '✓ Contraseña media';
        pwStrengthEl.style.color = 'var(--color-accent)';
      } else {
        message = '✓ Contraseña fuerte';
        pwStrengthEl.style.color = 'var(--color-accent)';
      }
      pwStrengthEl.textContent = message;
    });
  
    // ---- SAN format validation ----
    function validateSanFormat(san) {
      if (!san) return true; // vacío es válido (opcional)
      // Validar formato: dominio o IP sin prefijo, DNS:xxx o IP:xxx
      const prefixedRegex = /^(DNS|IP):(.+)$/i;
      const domainRegex = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$/i;
      const ipv6Regex = /^[0-9a-f:]+$/i;
      return prefixedRegex.test(san) || domainRegex.test(san) || (san.indexOf(':') !== -1 && ipv6Regex.test(san));
    }
  
    function getSanValues() {
      return Array.prototype.map.call(sanList.querySelectorAll('input'), function (i) { return i.value.trim(); })
        .filter(Boolean);
    }
  
    function setGenerating(isGenerating) {
      generateBtn.disabled = isGenerating;
      generateBtn.textContent = isGenerating ? 'Generando…' : 'Generar CSR';
    }
  
    csrForm.addEventListener('submit', async function (e) {
      e.preventDefault();
      formError.classList.remove('visible');
      resultPanel.classList.remove('visible');
  
      // ---- Validación de contraseña ----
      const pw = document.getElementById('pw').value;
      const pw2 = document.getElementById('pw2').value;
      const minPwLength = 8;
      
      if (pw.length < minPwLength) {
        formError.textContent = `La contraseña debe tener al menos ${minPwLength} caracteres`;
        formError.classList.add('visible');
        return;
      }
      
      if (pw !== pw2) {
        formError.textContent = 'Las contraseñas no coinciden';
        formError.classList.add('visible');
        return;
      }
  
      // ---- Validación de SAN ----
      const sanValues = getSanValues();
      for (let i = 0; i < sanValues.length; i++) {
        if (!validateSanFormat(sanValues[i])) {
          formError.textContent = 'SAN inválido: usar ejemplo.com o 10.0.0.1 (los prefijos DNS: e IP: son opcionales)';
          formError.classList.add('visible');
          return;
        }
      }
  
      const keyType = document.querySelector('input[name="keytype"]:checked').value;
      const payload = {
        cn: document.getElementById('cn').value,
        o: document.getElementById('o').value,
        ou: document.getElementById('ou').value,
        c: document.getElementById('c').value,
        st: document.getElementById('st').value,
        l: document.getElementById('l').value,
        sans: sanValues,
        keyType: keyType,
        password: pw,
        passwordConfirm: pw2
      };
      if (keyType === 'RSA') {
        payload.keySize = document.querySelector('input[name="keysize"]:checked').value;
      } else {
        payload.curve = document.querySelector('input[name="curve"]:checked').value;
      }
  
      setGenerating(true);
      try {
        const res = await fetch('/api/csr/generar', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('generation failed');
        const data = await res.json();
  
        const csrBlob = new Blob([data.csr], { type: 'application/x-pem-file' });
        const keyBlob = new Blob([data.keyEncrypted], { type: 'application/x-pem-file' });
        const cn = payload.cn || 'request';
  
        downloadCsr.href = URL.createObjectURL(csrBlob);
        downloadCsr.download = cn + '.csr';
        downloadKey.href = URL.createObjectURL(keyBlob);
        downloadKey.download = cn + '.key';
  
        resultPanel.classList.add('visible');
        
        // ---- Recargar historial automáticamente ----
        setTimeout(function () {
          loadHistory();
        }, 500);
      } catch (err) {
        formError.textContent = 'No se pudo generar el CSR. Intenta de nuevo.';
        formError.classList.add('visible');
      } finally {
        setGenerating(false);
      }
    });
  
  }
})();
