import VueRouter from "vue-router";

export function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj)) as T;
}

export function getStatusColor(status: string): string {
  var returnColor: string = "";
  if (!status) {
    return "backdrops lighten-1";
  }
  if (status.toLowerCase() == "failed") {
    returnColor = "error";
  } else if (status.toLowerCase() == "pending" || status.toLowerCase() == "in progress") {
    returnColor = "info";
  } else if (status.toLowerCase() == "active") {
    returnColor = "success";
  } else {
    returnColor = "warning";
  }
  return returnColor;
}

export function getHttpPostErrorNotice(httpStatusCode: number, vueRouter: VueRouter): string {
  if (httpStatusCode >= 400 && httpStatusCode < 500) {
    if (httpStatusCode == 401) {
      return "You are unauthorized for these actions. If you should have access, try logging in again";
    }
    return "Unexpected Error. Please contact support.";
  }
  return "Unexpected server error. If this problem persists please contact support.";
}

export function saveFile(filename: string, blob: Blob): void {
  var downloadLink = document.createElement("a");
  downloadLink.download = filename;
  try {
    downloadLink.href = window.URL.createObjectURL(blob);
    downloadLink.style.display = "none";
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
    // }
  } finally {
    //make sure we clean up
    URL.revokeObjectURL(downloadLink.href);
  }
}

export function copyToClipboard(text: string): void {
  let el = document.createElement("textarea");
  el.value = text;
  el.setAttribute("readonly", "");
  el.style.position = "absolute";
  el.style.left = "-9999px";

  document.body.appendChild(el);
  el.select();
  if (navigator.clipboard) {
    navigator.clipboard.writeText(el.value);
  }
  document.body.removeChild(el);
}
