import { AsyncPipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { Component, inject } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { Observable } from "rxjs";

@Component({
  selector: "app-root",
  imports: [RouterOutlet, AsyncPipe],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.css"
})
export class AppComponent {
  title = "quinoa-app";
  protected message: Observable<string> = inject(HttpClient).get(
    "/bar/foo/api/quinoa",
    {
      responseType: "text",
    }
  );
}
