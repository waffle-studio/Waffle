# -*- encoding: utf-8 -*-
# stub: sourcify 0.5.0 ruby lib

Gem::Specification.new do |s|
  s.name = "sourcify".freeze
  s.version = "0.5.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["NgTzeYang".freeze]
  s.date = "2011-05-02"
  s.description = "".freeze
  s.email = ["ngty77@gmail.com".freeze]
  s.extra_rdoc_files = ["README.rdoc".freeze]
  s.files = ["README.rdoc".freeze]
  s.homepage = "http://github.com/ngty/sourcify".freeze
  s.rubygems_version = "2.7.10".freeze
  s.summary = "Workarounds before ruby-core officially supports Proc#to_source (& friends)".freeze

  s.installed_by_version = "2.7.10" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ruby2ruby>.freeze, [">= 1.2.5"])
      s.add_runtime_dependency(%q<sexp_processor>.freeze, [">= 3.0.5"])
      s.add_runtime_dependency(%q<ruby_parser>.freeze, [">= 2.0.5"])
      s.add_runtime_dependency(%q<file-tail>.freeze, [">= 1.0.5"])
      s.add_development_dependency(%q<bacon>.freeze, [">= 0"])
    else
      s.add_dependency(%q<ruby2ruby>.freeze, [">= 1.2.5"])
      s.add_dependency(%q<sexp_processor>.freeze, [">= 3.0.5"])
      s.add_dependency(%q<ruby_parser>.freeze, [">= 2.0.5"])
      s.add_dependency(%q<file-tail>.freeze, [">= 1.0.5"])
      s.add_dependency(%q<bacon>.freeze, [">= 0"])
    end
  else
    s.add_dependency(%q<ruby2ruby>.freeze, [">= 1.2.5"])
    s.add_dependency(%q<sexp_processor>.freeze, [">= 3.0.5"])
    s.add_dependency(%q<ruby_parser>.freeze, [">= 2.0.5"])
    s.add_dependency(%q<file-tail>.freeze, [">= 1.0.5"])
    s.add_dependency(%q<bacon>.freeze, [">= 0"])
  end
end
