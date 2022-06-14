import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'quinoa-app';
  message?: string = undefined;

  constructor(private http:HttpClient) { }

  ngOnInit():void {
    this.http.get("/bar/foo/api/quinoa", {responseType: 'text'}).subscribe(data => {
      this.message = data as string
    })
  }
}
