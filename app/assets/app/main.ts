import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app.module';

/*
TODO Produktivmodus einschalten

// depending on the env mode, enable prod mode or add debugging modules
if (process.env.ENV === 'build') {
  enableProdMode();
}
*/

const platform = platformBrowserDynamic();
platform.bootstrapModule(AppModule);
