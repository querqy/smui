import {FilterRule, RedirectRule} from "./rules.model";

export class DownloadableRule {

  type?: string;
  filterRule?: FilterRule;
  redirectRule?: RedirectRule;

  inputId?: string;
  inputTerm?: string;
  term?: string;
  details?: string;
  isActive?: boolean;
  inputTags: Array<string>;
  modified: string;
  inputModified: string;
}
