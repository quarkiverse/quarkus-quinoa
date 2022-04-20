import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'quinoa-app';
  message = 'loading...'

  constructor(private http:HttpClient) { }

  ngOnInit():void {
    this.http.get("/api/quinoa", {responseType: 'text'}).subscribe(data => {
      this.message = data as string
    })
  }
}
