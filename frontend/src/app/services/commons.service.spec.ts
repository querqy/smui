import { TestBed } from '@angular/core/testing';

import { CommonsService } from './commons.service';
import {SimpleChange, SimpleChanges} from '@angular/core';

describe('CommonsService', () => {
  let commonsService: CommonsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CommonsService]
    });
    commonsService = TestBed.inject(CommonsService);
  });

  it('should be created', () => {
    expect(commonsService).toBeTruthy();
  });

  it('generate a unique identifier', () => {
    const id = commonsService.generateUUID();
    const regex = new RegExp('^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$');
    expect(regex.test(id)).toBeTrue();
  });

  it('return if a object is dirty', () => {
    const objOrig = { a: 1, b: { c: 2 } };
    const objOrigStr =  JSON.stringify(objOrig);
    const objChanged = { ...objOrig, a: 2 };
    const objChangedDeep = { ...objOrig, b: { c: 3 } };

    expect(commonsService.isDirty(objOrig, objOrigStr)).toBeFalse();
    expect(commonsService.isDirty(objChanged, objOrigStr)).toBeTrue();
    expect(commonsService.isDirty(objChangedDeep, objOrigStr)).toBeTrue();
  });

  it('return if the state for a field (SimpleChanges) has changed', () => {
    const changes = {
      someField: new SimpleChange('A', 'B', true),
      someOtherField: new SimpleChange('A', 'A', true)
    };

    expect(commonsService.hasChanged(changes, 'someField')).toBeTrue();
    expect(commonsService.hasChanged(changes, 'someOtherField')).toBeFalse();
  });

  it('remove quotes from a string', () => {
    expect(commonsService.removeQuotes('"noQuotes"')).toBe('noQuotes');
    expect(commonsService.removeQuotes('"no"Qu""o"t"es"')).toBe('noQuotes');
    expect(commonsService.removeQuotes('noQu\'ot\'es')).toBe('noQuotes');
  });
});
