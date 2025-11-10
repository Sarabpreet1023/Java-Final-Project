<!-- place inside admin dashboard JSP where admin controls are shown -->
<div style="margin:18px 0;">
  <label>Reset show (admin)</label>
  <input id="resetShowId" value="1001" style="width:90px; padding:6px; margin-right:8px;" />
  <button id="btnResetShow" style="background:#d9534f;color:white;padding:8px 12px;border-radius:6px;border:0;cursor:pointer;">
    Reset Show (delete bookings + reset XML)
  </button>
  <span id="resetResult" style="margin-left:12px;font-weight:700;"></span>
</div>

<script>
document.getElementById('btnResetShow').addEventListener('click', async function() {
  if(!confirm('Are you sure you want to reset the show (delete bookings/payments) AND rewrite seats XML?')) return;
  const id = document.getElementById('resetShowId').value.trim();
  if (!id) { alert('Enter show id'); return; }

  const resultEl = document.getElementById('resetResult');
  resultEl.style.color = 'black';
  resultEl.innerText = 'Resetting...';
  try {
    const params = new URLSearchParams();
    params.append('showId', id);

    const resp = await fetch('<%= request.getContextPath() %>/admin/resetShow', {
      method: 'POST',
      headers: { 'Content-Type':'application/x-www-form-urlencoded' },
      body: params.toString()
    });

    const text = await resp.text();
    if (resp.ok) {
      resultEl.style.color = 'green';
      resultEl.innerText = 'Reset OK for show ' + id + '. Reloading seat map...';
      // reload seatmap page for immediate verification
      setTimeout(function(){ window.location.href = '<%= request.getContextPath() %>/seatmap?showId=' + encodeURIComponent(id); }, 900);
    } else {
      resultEl.style.color = 'red';
      // try to present server error JSON
      try { const j = JSON.parse(text); resultEl.innerText = 'Error: ' + (j.message || text); }
      catch(e) { resultEl.innerText = 'Error: ' + text; }
    }
  } catch (err) {
    resultEl.style.color = 'red';
    resultEl.innerText = 'Network error: ' + err.message;
  }
});
</script>
