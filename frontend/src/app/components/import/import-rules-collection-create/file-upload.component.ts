import {Component, ElementRef, ViewChild} from "@angular/core";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'file-upload',
  templateUrl: "file-upload.component.html",
  styleUrls: ["file-upload.component.scss"]
})
export class FileUploadComponent {
  fileName = '';
  target: EventTarget | null;

  @ViewChild('fileUpload')
  myInputVariable: ElementRef;

  constructor(private http: HttpClient) {
  }

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files != null) {
      const file: File = target.files[0];
      if (file) {
        this.fileName = file.name;
        const formData = new FormData();
        formData.append("uploadedFile", file);
        const upload$ = this.http.post("/api/v1/upload-import", formData);
        upload$.subscribe();
        this.fileName = 'Last file uploaded: ' + this.fileName;
        this.myInputVariable.nativeElement.value = '';
      }
    }
  }
}