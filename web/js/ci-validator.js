window.CiUyValidator = (function () {
  const MULTIPLIERS = [2, 9, 8, 7, 6, 3, 4];

  function clean(ci) {
    return String(ci || "").replace(/\D/g, "");
  }

  function validationDigit(numberWithoutCheck) {
    let sum = 0;
    for (let i = 0; i < numberWithoutCheck.length; i++) {
      sum += Number(numberWithoutCheck.charAt(i)) * MULTIPLIERS[i % MULTIPLIERS.length];
    }
    const mod = sum % 10;
    return mod === 0 ? 0 : 10 - mod;
  }

  function isValid(ci) {
    const digits = clean(ci);
    if (digits.length < 6 || digits.length > 8) {
      return false;
    }
    const body = digits.slice(0, -1);
    const check = Number(digits.slice(-1));
    return validationDigit(body) === check;
  }

  function format(ci) {
    const digits = clean(ci);
    if (digits.length <= 1) {
      return digits;
    }
    const body = digits.slice(0, -1);
    const check = digits.slice(-1);
    if (body.length <= 3) {
      return body + "-" + check;
    }
    if (body.length <= 6) {
      return body.slice(0, 1) + "." + body.slice(1, 4) + "." + body.slice(4) + "-" + check;
    }
    return body.slice(0, 1) + "." + body.slice(1, 4) + "." + body.slice(4, 7) + "-" + check;
  }

  return { clean, isValid, format };
})();
